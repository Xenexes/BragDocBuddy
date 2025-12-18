package infrastructure.ai

import ApplicationConfiguration
import domain.ai.PromptTemplate
import domain.ai.dto.TemplateType
import domain.config.OllamaConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import ports.TemplateService
import java.io.File
import java.nio.file.Paths

class FileTemplateService(
    private val appConfiguration: ApplicationConfiguration,
    private val ollamaConfiguration: OllamaConfiguration,
) : TemplateService {
    private val logger = KotlinLogging.logger {}
    private val templatesCache = mutableMapOf<TemplateType, PromptTemplate>()

    override fun loadTemplate(type: TemplateType): PromptTemplate {
        templatesCache[type]?.let { return it }

        val content =
            loadFromCustomDirectory(type)
                ?: loadFromBragDocFolder(type)
                ?: loadFromResources(type)
                ?: throw TemplateNotFoundException(
                    "Template ${type.fileName} not found in brag doc folder, custom directory, or embedded resources. " +
                        "This should not happen - embedded resources may be missing.",
                )

        val template = PromptTemplate(type, content)

        val validationResult = template.validate()
        if (!validationResult.isValid()) {
            throw TemplateValidationException(
                "Template validation failed for ${type.fileName}: ${validationResult.errorMessage()}",
            )
        }

        templatesCache[type] = template
        logger.info { "Loaded template: ${type.fileName}" }

        return template
    }

    private fun loadFromBragDocFolder(type: TemplateType): String? {
        val templateFile = File(appConfiguration.docsLocation, ".templates/${type.fileName}")
        if (!templateFile.exists()) {
            logger.debug { "Brag doc template not found: ${templateFile.absolutePath}" }
            return null
        }

        logger.info { "Loading custom template from brag doc folder: ${templateFile.absolutePath}" }
        return templateFile.readText()
    }

    private fun loadFromCustomDirectory(type: TemplateType): String? {
        val customDir = ollamaConfiguration.templatesDir ?: return null

        val templateFile = Paths.get(customDir, type.fileName).toFile()
        if (!templateFile.exists()) {
            logger.debug { "Custom directory template not found: ${templateFile.absolutePath}" }
            return null
        }

        logger.info { "Loading custom template from custom directory: ${templateFile.absolutePath}" }
        return templateFile.readText()
    }

    override fun loadFromResources(type: TemplateType): String? {
        val resourcePath = "$RESOURCE_TEMPLATES_PATH/${type.fileName}"

        return try {
            // Use classloader to read resource (works in JAR and native-image)
            val inputStream =
                this::class.java.classLoader.getResourceAsStream(resourcePath)
                    ?: throw TemplateNotFoundException("Resource not found: $resourcePath")

            val content = inputStream.bufferedReader().use { it.readText() }
            logger.debug { "Loaded embedded template: ${type.fileName}" }
            content
        } catch (e: Exception) {
            logger.warn { "Failed to load embedded template ${type.fileName}: ${e.message}" }
            null
        }
    }

    override fun listAvailableTemplates(): List<TemplateType> =
        TemplateType.entries.filter { type ->
            loadFromBragDocFolder(type) != null ||
                loadFromCustomDirectory(type) != null ||
                loadFromResources(type) != null
        }

    override fun exportTemplatesToBragDocFolder(): ports.TemplateExportResult {
        val targetDir = File(appConfiguration.docsLocation, ".templates")
        return exportTemplatesToDirectory(targetDir.absolutePath)
    }

    override fun exportTemplatesToDirectory(targetDir: String): ports.TemplateExportResult {
        val dir = Paths.get(targetDir).toFile()
        if (!dir.exists()) {
            dir.mkdirs()
            logger.info { "Created templates directory: ${dir.absolutePath}" }
        }

        var exportedCount = 0
        var overwrittenCount = 0
        var failedCount = 0
        val exportedTemplates = mutableListOf<String>()
        val overwrittenTemplates = mutableListOf<String>()
        val failedTemplates = mutableListOf<Pair<String, String>>()

        TemplateType.entries.forEach { type ->
            val targetFile = File(dir, type.fileName)
            val alreadyExists = targetFile.exists()

            try {
                val content = loadFromResources(type)
                if (content != null) {
                    targetFile.writeText(content)

                    if (alreadyExists) {
                        overwrittenCount++
                        overwrittenTemplates.add(type.fileName)
                        logger.info { "Overwritten template: ${targetFile.absolutePath}" }
                    } else {
                        exportedCount++
                        exportedTemplates.add(type.fileName)
                        logger.info { "Exported template: ${targetFile.absolutePath}" }
                    }
                } else {
                    failedCount++
                    failedTemplates.add(type.fileName to "Embedded resource not found")
                    logger.error { "Failed to export ${type.fileName} - resource not found" }
                }
            } catch (e: Exception) {
                failedCount++
                failedTemplates.add(type.fileName to (e.message ?: "Unknown error"))
                logger.error(e) { "Failed to export ${type.fileName}" }
            }
        }

        return ports.TemplateExportResult(
            exportedCount = exportedCount,
            overwrittenCount = overwrittenCount,
            failedCount = failedCount,
            targetDirectory = dir.absolutePath,
            exportedTemplates = exportedTemplates,
            overwrittenTemplates = overwrittenTemplates,
            failedTemplates = failedTemplates,
        )
    }

    override fun isCustomTemplate(type: TemplateType): Boolean {
        val bragDocPath = File(appConfiguration.docsLocation, ".templates/${type.fileName}")
        if (bragDocPath.exists()) return true

        val customDir = ollamaConfiguration.templatesDir ?: return false
        val customPath = Paths.get(customDir, type.fileName).toFile()
        return customPath.exists()
    }

    override fun getCustomTemplatePath(type: TemplateType): String? {
        val bragDocPath = File(appConfiguration.docsLocation, ".templates/${type.fileName}")
        if (bragDocPath.exists()) return bragDocPath.absolutePath

        val customDir = ollamaConfiguration.templatesDir ?: return null
        val customPath = Paths.get(customDir, type.fileName).toFile()
        return if (customPath.exists()) customPath.absolutePath else null
    }

    override fun clearCache() {
        templatesCache.clear()
        logger.debug { "Template cache cleared" }
    }

    companion object {
        const val RESOURCE_TEMPLATES_PATH = "templates"
    }
}

class TemplateNotFoundException(
    message: String,
) : Exception(message)

class TemplateValidationException(
    message: String,
) : Exception(message)
