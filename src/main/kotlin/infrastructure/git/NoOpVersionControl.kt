package infrastructure.git

import ports.VersionControl
import java.io.File

class NoOpVersionControl : VersionControl {
    override fun commitAndPush(
        file: File,
        message: String,
    ): Boolean = true
}
