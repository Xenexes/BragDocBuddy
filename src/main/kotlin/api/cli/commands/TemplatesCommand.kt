package api.cli.commands

import domain.ai.dto.TemplateType
import ports.TemplateService

/**
 * Command to manage AI prompt templates.
 *
 * This command is responsible ONLY for presentation logic (CLI output).
 * All infrastructure operations (file I/O, template loading) are delegated
 * to the TemplateService port.
 *
 * Usage: brag-doc templates [list|show|export]
 */
class TemplatesCommand(
    private val templateService: TemplateService,
    private val args: List<String>,
) : Command {
    override fun execute() {
        when {
            args.isEmpty() || args[0] == "list" -> listTemplates()
            args[0] == "show" && args.size > 1 -> showTemplate(args[1])
            args[0] == "export" -> exportTemplates()
            else -> printUsage()
        }
    }

    private fun listTemplates() {
        println("Available Templates:")
        println("=" * 80)
        println()

        TemplateType.entries.forEach { type ->
            val displayName = type.name.lowercase().replace('_', '-')
            println("  $displayName")
            println("    File: ${type.fileName}")

            if (templateService.isCustomTemplate(type)) {
                val customPath = templateService.getCustomTemplatePath(type)
                println("    Status: Custom (overriding default)")
                println("    Location: $customPath")
            } else {
                println("    Status: Default (embedded)")
            }
            println()
        }

        println("=" * 80)
        println()
        println("Commands:")
        println("  brag-doc templates show <type>    - View a template")
        println("  brag-doc templates export          - Export templates for customization")
        println()
        println("Available types: ${TemplateType.entries.joinToString(", ") { it.name.lowercase().replace('_', '-') }}")
    }

    private fun showTemplate(typeName: String) {
        val type = findTemplateType(typeName)
        if (type == null) {
            println("Error: Unknown template type '$typeName'")
            println()
            println(
                "Available types: ${
                    TemplateType.entries.joinToString(", ") {
                        it.name.lowercase().replace('_', '-')
                    }
                }",
            )
            return
        }

        try {
            val template = templateService.loadTemplate(type)
            val displayName = type.name.lowercase().replace('_', '-')
            val isCustom = templateService.isCustomTemplate(type)

            println("Template: $displayName")
            if (isCustom) {
                val customPath = templateService.getCustomTemplatePath(type)
                println("Source: Custom ($customPath)")
            } else {
                println("Source: Default (embedded)")
            }
            println("=" * 80)
            println()
            println(template.content)
            println()
            println("=" * 80)
            println()
            println("Placeholders used:")
            println("  {timeframe} - Time period (e.g., 'Q4 2024', '2023')")
            println("  {count}     - Number of entries")
            println("  {entries}   - List of brag entries")
            println()

            if (!isCustom) {
                val customPath = templateService.getCustomTemplatePath(type)
                println("To customize this template:")
                println("  1. Run: brag-doc templates export")
                println("  2. Edit: $customPath")
                println("  3. Your custom version will be used automatically")
            }
        } catch (e: Exception) {
            println("Error loading template: ${e.message}")
        }
    }

    private fun exportTemplates() {
        val result = templateService.exportTemplatesToBragDocFolder()

        println("Templates directory: ${result.targetDirectory}")
        println()

        result.exportedTemplates.forEach { fileName ->
            println("✓ $fileName - Exported")
        }

        result.overwrittenTemplates.forEach { fileName ->
            println("✓ $fileName - Overwritten with latest default")
        }

        result.failedTemplates.forEach { (fileName, error) ->
            println("✗ $fileName - Failed: $error")
        }

        println()
        println("=" * 80)
        println("Exported: ${result.exportedCount} new template(s)")
        if (result.overwrittenCount > 0) {
            println("Overwritten: ${result.overwrittenCount} template(s) with latest defaults")
        }
        if (result.failedCount > 0) {
            println("Failed: ${result.failedCount} template(s)")
        }
        println()
        println("Templates location: ${result.targetDirectory}")
        println()
        println("You can now edit these templates to customize the AI prompts.")
        println("Your custom templates will be used automatically.")
        println()
        println("To revert to defaults, run 'templates export' again.")
    }

    private fun findTemplateType(name: String): TemplateType? {
        val normalizedName = name.uppercase().replace('-', '_')
        return TemplateType.entries.find { it.name == normalizedName }
    }

    private fun printUsage() {
        println("Usage: brag-doc templates <command>")
        println()
        println("Commands:")
        println("  list              - List all available templates (default)")
        println("  show <type>       - Display a specific template")
        println("  export            - Export template to .templates/ folder")
        println()
        println("Examples:")
        println("  brag-doc templates list")
        println("  brag-doc templates show performance-review")
        println("  brag-doc templates export")
    }
}

private operator fun String.times(count: Int): String = this.repeat(count)
