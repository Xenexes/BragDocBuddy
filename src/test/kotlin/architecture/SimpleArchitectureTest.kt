package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.ext.list.withText
import com.lemonappdev.konsist.api.ext.list.withoutNameEndingWith
import com.lemonappdev.konsist.api.ext.list.withoutPackage
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

class SimpleArchitectureTest {
    @Test
    fun `use cases should be in usecases package`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withPackage("..usecases..")
            .withoutNameEndingWith("Test")
            .assertTrue { it.name.endsWith("UseCase") }
    }

    @Test
    fun `repository implementations should be in infrastructure persistence package`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withPackage("..infrastructure.persistence..")
            .assertTrue { it.name.endsWith("Repository") }
    }

    @Test
    fun `Koin components should only be in API layer`() {
        Konsist
            .scopeFromProject()
            .files
            .withText { it.contains("KoinComponent") }
            .withoutPackage("..architecture..")
            .assertTrue { it.packagee?.name?.startsWith("api") == true }
    }

    @Test
    fun `dependency injection should only be in API layer`() {
        Konsist
            .scopeFromProject()
            .files
            .withText { it.contains("by inject()") }
            .withoutPackage("..architecture..")
            .assertTrue { it.packagee?.name?.startsWith("api") == true }
    }

    @Test
    fun `domain layer should not have framework dependencies`() {
        Konsist
            .scopeFromProject()
            .files
            .withPackage("..domain..")
            .assertTrue {
                !it.text.contains("org.koin") &&
                    !it.text.contains("KoinComponent") &&
                    !it.text.contains("inject()")
            }
    }

    @Test
    fun `use cases should not have framework dependencies`() {
        Konsist
            .scopeFromProject()
            .files
            .withPackage("..usecases..")
            .assertTrue {
                !it.text.contains("org.koin") &&
                    !it.text.contains("KoinComponent") &&
                    !it.text.contains("inject()")
            }
    }

    @Test
    fun `ports should not have framework dependencies`() {
        Konsist
            .scopeFromProject()
            .files
            .withPackage("..ports..")
            .assertTrue {
                !it.text.contains("org.koin") &&
                    !it.text.contains("KoinComponent") &&
                    !it.text.contains("inject()")
            }
    }
}
