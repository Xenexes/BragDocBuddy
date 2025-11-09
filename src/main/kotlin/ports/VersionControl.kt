package ports

import java.io.File

interface VersionControl {
    fun commitAndPush(
        file: File,
        message: String,
    ): Boolean
}
