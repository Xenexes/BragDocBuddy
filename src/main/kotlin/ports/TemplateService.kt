package ports

import domain.ai.PromptTemplate
import domain.ai.dto.TemplateType

interface TemplateService {
    fun loadTemplate(type: TemplateType): PromptTemplate

    fun loadFromResources(type: TemplateType): String?

    fun listAvailableTemplates(): List<TemplateType>

    fun exportTemplatesToBragDocFolder(): TemplateExportResult

    fun exportTemplatesToDirectory(targetDir: String): TemplateExportResult

    fun isCustomTemplate(type: TemplateType): Boolean

    fun getCustomTemplatePath(type: TemplateType): String?

    fun clearCache()
}

data class TemplateExportResult(
    val exportedCount: Int,
    val overwrittenCount: Int,
    val failedCount: Int,
    val targetDirectory: String,
    val exportedTemplates: List<String>,
    val overwrittenTemplates: List<String>,
    val failedTemplates: List<Pair<String, String>>, // filename to error message
)
