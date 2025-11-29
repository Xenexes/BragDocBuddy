package ports

interface CurrentVersionProvider {
    fun getCurrentVersion(): String
}
