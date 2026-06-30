package com.backupx.app.helper

/**
 * Windows-specific helper functions
 */
object WindowsHelper {

    /**
     * Open a file or folder in Windows Explorer
     */
    fun openInExplorer(path: String) {
        try {
            Runtime.getRuntime().exec(arrayOf("explorer.exe", path))
        } catch (e: Exception) {
            println("Error opening in Explorer: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Show the native Windows folder browser and return the selected path
     * Returns null when the user cancels the dialog
     */
    fun pickFolder(title: String): String? {
        return try {
            // force utf-8 output so paths with accented characters survive the read
            val script = """
                [Console]::OutputEncoding = New-Object System.Text.UTF8Encoding ${'$'}false
                Add-Type -AssemblyName System.Windows.Forms
                ${'$'}dialog = New-Object System.Windows.Forms.FolderBrowserDialog
                ${'$'}dialog.Description = '$title'
                if (${'$'}dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) { Write-Output ${'$'}dialog.SelectedPath }
            """.trimIndent()

            val process = ProcessBuilder("powershell.exe", "-NoProfile", "-STA", "-Command", script)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()

            // read stdout as utf-8 before waitFor to prevent buffer deadlock
            val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText().trim()
            process.waitFor()

            output.ifBlank { null }
        } catch (e: Exception) {
            println("Error opening folder dialog: ${e.message}")
            null
        }
    }

    /**
     * Store a secret encrypted with the current user's data protection key
     * The value is read from stdin and the ciphertext is written to a per-user file
     */
    fun storeSecret(account: String, secret: String): Boolean {
        return try {
            val script = """
                ${'$'}plain = [Console]::In.ReadToEnd()
                ${'$'}enc = ConvertTo-SecureString -String ${'$'}plain -AsPlainText -Force | ConvertFrom-SecureString
                ${'$'}dir = Join-Path ${'$'}env:APPDATA 'BackupX\secrets'
                New-Item -ItemType Directory -Force -Path ${'$'}dir | Out-Null
                Set-Content -Path (Join-Path ${'$'}dir '$account.dat') -Value ${'$'}enc -NoNewline
            """.trimIndent()

            val process = ProcessBuilder("powershell.exe", "-NoProfile", "-Command", script)
                .redirectErrorStream(true)
                .start()
            process.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(secret) }
            process.inputStream.bufferedReader().readText()
            process.waitFor() == 0
        } catch (e: Exception) {
            println("Error storing secret: ${e.message}")
            false
        }
    }

    /**
     * Read a secret encrypted with the current user's data protection key, returning null when absent
     */
    fun loadSecret(account: String): String? {
        return try {
            val script = """
                [Console]::OutputEncoding = New-Object System.Text.UTF8Encoding ${'$'}false
                ${'$'}path = Join-Path ${'$'}env:APPDATA 'BackupX\secrets\$account.dat'
                if (Test-Path ${'$'}path) {
                    ${'$'}sec = Get-Content ${'$'}path | ConvertTo-SecureString
                    ${'$'}ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR(${'$'}sec)
                    [Runtime.InteropServices.Marshal]::PtrToStringAuto(${'$'}ptr)
                }
            """.trimIndent()

            val process = ProcessBuilder("powershell.exe", "-NoProfile", "-Command", script).start()
            val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText().trim()
            if (process.waitFor() == 0 && output.isNotEmpty()) output else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Remove the stored secret file for the account
     */
    fun deleteSecret(account: String) {
        try {
            val script = """
                ${'$'}path = Join-Path ${'$'}env:APPDATA 'BackupX\secrets\$account.dat'
                if (Test-Path ${'$'}path) { Remove-Item ${'$'}path -Force }
            """.trimIndent()
            ProcessBuilder("powershell.exe", "-NoProfile", "-Command", script).start().waitFor()
        } catch (e: Exception) {
            println("Error deleting secret: ${e.message}")
        }
    }
}
