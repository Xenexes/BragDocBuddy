package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
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
            .withNameEndingWith("Repository")
            .withoutNameEndingWith("Test")
            .assertTrue { it.resideInPackage("..infrastructure.persistence..") }
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

    @Test
    fun `use cases must depend only on ports not concrete infrastructure implementations`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withPackage("..usecases..")
            .withNameEndingWith("UseCase")
            .assertTrue { useCase ->
                val imports = useCase.containingFile.imports.map { it.name }

                val hasInfrastructureImport =
                    imports.any { import ->
                        import.startsWith("infrastructure.") &&
                            !import.contains("Test")
                    }

                !hasInfrastructureImport
            }
    }

    @Test
    fun `API commands must not perform file I-O operations`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withPackage("..api.cli.commands..")
            .withNameEndingWith("Command")
            .assertTrue { command ->
                val fileContent = command.containingFile.text

                val hasFileImport =
                    fileContent.contains("import java.io.File") ||
                        fileContent.contains("import java.io.*")

                val hasFileOperations =
                    fileContent.contains("File(") ||
                        fileContent.contains(".mkdirs()") ||
                        fileContent.contains(".writeText(") ||
                        fileContent.contains(".readText(")

                !hasFileImport && !hasFileOperations
            }
    }

    @Test
    fun `Main-kt must not import concrete infrastructure implementations`() {
        Konsist
            .scopeFromProject()
            .files
            .assertTrue { file ->
                if (file.name == "Main") {
                    val imports = file.imports.map { it.name }

                    // Main.kt should not import concrete infrastructure classes
                    val hasConcreteInfraImport =
                        imports.any { import ->
                            import.startsWith("infrastructure.") &&
                                !import.contains("Test")
                        }

                    !hasConcreteInfraImport
                } else {
                    true
                }
            }
    }
}
