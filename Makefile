# detect the host platform and pick the matching gradle wrapper
ifeq ($(OS),Windows_NT)
	GRADLEW := gradlew.bat
	PLATFORM := windows
else
	GRADLEW := ./gradlew
	ifeq ($(shell uname -s),Darwin)
		PLATFORM := macos
	else
		PLATFORM := linux
	endif
endif

.DEFAULT_GOAL := help
.PHONY: help run build dist package clean

help:
	@echo BackupX - host platform: $(PLATFORM)
	@echo Available targets:
	@echo   make run     - run the app
	@echo   make build   - compile and assemble the project
	@echo   make dist    - create the runnable application image
	@echo   make package - build the native installer for this platform
	@echo   make clean    - remove build outputs

run:
	$(GRADLEW) :composeApp:run

build:
	$(GRADLEW) :composeApp:build

dist:
	$(GRADLEW) :composeApp:createDistributable

# packages the native installer for the current host (dmg, msi or deb)
package:
	$(GRADLEW) :composeApp:packageDistributionForCurrentOS

clean:
	$(GRADLEW) clean
