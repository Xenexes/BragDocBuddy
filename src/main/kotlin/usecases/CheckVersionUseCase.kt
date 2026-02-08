package usecases

import ports.VersionChecker

class CheckVersionUseCase(
    private val versionChecker: VersionChecker,
) {
    fun checkForUpdates() {
        versionChecker.checkForUpdates()
    }
}
