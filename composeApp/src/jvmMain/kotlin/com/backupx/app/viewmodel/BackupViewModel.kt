package com.backupx.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.backupx.app.helper.SecretStoreHelper
import com.backupx.app.model.BackupConfig
import com.backupx.app.model.BackupItem
import com.backupx.app.model.BackupProviderType
import com.backupx.app.model.S3Settings
import com.backupx.app.provider.BackupException
import com.backupx.app.provider.BackupProviderFactory
import com.backupx.app.provider.ConnectionTestResult
import com.backupx.app.provider.S3BackupProvider
import com.backupx.app.repository.BackupRepository
import com.backupx.composeapp.generated.resources.Res
import com.backupx.composeapp.generated.resources.error_unknown
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class BackupViewModel(
    private val repository: BackupRepository,
    private val providerFactory: BackupProviderFactory
) : ViewModel() {

    private val _items = MutableStateFlow<List<BackupItem>>(emptyList())
    val items: StateFlow<List<BackupItem>> = _items

    private val _selectedId = MutableStateFlow<String?>(null)
    val selectedId: StateFlow<String?> = _selectedId

    private val _statuses = MutableStateFlow<Map<String, BackupRunStatus>>(emptyMap())
    val statuses: StateFlow<Map<String, BackupRunStatus>> = _statuses

    private val _isRunningAll = MutableStateFlow(false)
    val isRunningAll: StateFlow<Boolean> = _isRunningAll

    private val _overallProgress = MutableStateFlow(0f)
    val overallProgress: StateFlow<Float> = _overallProgress

    init {
        setItems(repository.load().items)
    }

    fun selectItem(id: String) {
        _selectedId.value = if (_selectedId.value == id) null else id
    }

    fun newItemId(): String = UUID.randomUUID().toString()

    fun saveItem(item: BackupItem, secret: String?) {
        // sensitive secrets go to the system secret store, never to the config file
        // a null secret means this provider has none, so drop any leftover entry for the id
        if (secret != null) SecretStoreHelper.store(item.id, secret) else SecretStoreHelper.delete(item.id)

        val existing = _items.value.any { it.id == item.id }
        setItems(
            if (existing) {
                _items.value.map { if (it.id == item.id) item else it }
            } else {
                _items.value + item
            }
        )

        clearStatus(item.id)
        persist()
    }

    fun deleteItem(id: String) {
        setItems(_items.value.filterNot { it.id == id })
        if (_selectedId.value == id) _selectedId.value = null

        SecretStoreHelper.delete(id)
        clearStatus(id)
        persist()
    }

    fun loadSecret(id: String): String? = SecretStoreHelper.load(id)

    suspend fun testS3(settings: S3Settings, secret: String): ConnectionTestResult {
        val provider = providerFactory.resolve(BackupProviderType.s3) as S3BackupProvider
        return provider.test(settings, secret)
    }

    fun runItem(id: String) {
        val item = _items.value.find { it.id == id } ?: return

        viewModelScope.launch(Dispatchers.IO) {
            runSingle(item)
        }
    }

    fun runAll() {
        val items = _items.value
        if (items.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isRunningAll.value = true
            _overallProgress.value = 0f

            // run every backup and keep going even when one fails
            items.forEachIndexed { index, item ->
                runSingle(item)
                _overallProgress.value = (index + 1).toFloat() / items.size
            }

            _isRunningAll.value = false
        }
    }

    private suspend fun runSingle(item: BackupItem) {
        updateStatus(item.id, BackupRunStatus.Running(0f))

        try {
            val provider = providerFactory.resolve(item.settings.type)
            provider.execute(item) { progress ->
                updateStatus(item.id, BackupRunStatus.Running(progress))
            }
            updateStatus(item.id, BackupRunStatus.Success)
        } catch (e: CancellationException) {
            throw e
        } catch (e: BackupException) {
            updateStatus(item.id, BackupRunStatus.Failed(getString(e.resource)))
        } catch (e: Exception) {
            updateStatus(item.id, BackupRunStatus.Failed(e.message ?: getString(Res.string.error_unknown)))
        }
    }

    private fun setItems(items: List<BackupItem>) {
        // keep the list alphabetical by name, case-insensitive
        _items.value = items.sortedBy { it.name.lowercase() }
    }

    private fun updateStatus(id: String, status: BackupRunStatus) {
        // atomic so concurrent individual runs never drop each other's status
        _statuses.update { it + (id to status) }
    }

    private fun clearStatus(id: String) {
        _statuses.update { it - id }
    }

    private fun persist() {
        repository.save(BackupConfig(items = _items.value))
    }
}
