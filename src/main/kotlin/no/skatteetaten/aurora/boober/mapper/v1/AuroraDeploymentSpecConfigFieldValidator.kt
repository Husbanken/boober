package no.skatteetaten.aurora.boober.mapper.v1

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.skatteetaten.aurora.boober.mapper.AuroraConfigException
import no.skatteetaten.aurora.boober.mapper.AuroraConfigFieldHandler
import no.skatteetaten.aurora.boober.mapper.AuroraConfigFields
import no.skatteetaten.aurora.boober.model.ApplicationId
import no.skatteetaten.aurora.boober.model.AuroraConfigFile
import no.skatteetaten.aurora.boober.model.ConfigFieldErrorDetail
import no.skatteetaten.aurora.boober.utils.findAllPointers
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AuroraDeploymentSpecConfigFieldValidator(val applicationId: ApplicationId,
                                               val applicationFiles: List<AuroraConfigFile>,
                                               val fieldHandlers: Set<AuroraConfigFieldHandler>,
                                               val auroraConfigFields: AuroraConfigFields) {
    val logger: Logger = LoggerFactory.getLogger(AuroraDeploymentSpecConfigFieldValidator::class.java)

    companion object {
        val namePattern = "^[a-z][-a-z0-9]{0,38}[a-z0-9]$"
    }

    @JvmOverloads
    fun validate(fullValidation: Boolean = true) {

        val envPointers = listOf("env/name", "env/ttl", "envName", "affiliation",
                "permissions/admin", "permissions/view", "permissions/adminServiceAccount")

        val errors: List<ConfigFieldErrorDetail> = fieldHandlers.mapNotNull { e ->
            val rawField = auroraConfigFields.fields[e.name]!!

            val invalidEnvSource = envPointers.contains(e.name) && rawField.source?.name
                    ?.let { !it.split("/").last().startsWith("about") }
                    ?: false

            logger.trace("Validating field=${e.name}")
            val auroraConfigField: JsonNode? = rawField.valueOrDefault
            logger.trace("value is=${jacksonObjectMapper().writeValueAsString(auroraConfigField)}")

            val result = e.validator(auroraConfigField)
            logger.trace("validator result is=${result}")

            val err = when {
                invalidEnvSource -> ConfigFieldErrorDetail.illegal("Invalid Source field=${e.name} requires an about source. Actual source is source=${rawField.source?.name}", rawField)
                result == null -> null
                auroraConfigField != null -> ConfigFieldErrorDetail.illegal(result.localizedMessage, rawField)
                else -> ConfigFieldErrorDetail.missing(result.localizedMessage, e.path)
            }
            if (err != null) {
                logger.trace("Error=$err message=${err.message}")
            }
            err
        }

        val unmappedErrors = if (fullValidation) {
            getUnmappedPointers().flatMap { pointerError ->
                pointerError.value.map { ConfigFieldErrorDetail.invalid(pointerError.key, it) }
            }
        } else {
            emptyList()
        }

        (errors + unmappedErrors).takeIf { it.isNotEmpty() }?.let {
            val aid = applicationId
            throw AuroraConfigException(
                    "Config for application ${aid.application} in environment ${aid.environment} contains errors",
                    errors = it
            )
        }
    }

    private fun getUnmappedPointers(): Map<String, List<String>> {
        val allPaths = fieldHandlers.map { it.path }

        val filePointers = applicationFiles.associateBy({ it.configName }, { it.asJsonNode.findAllPointers(3) })

        return filePointers.mapValues { it.value - allPaths }.filterValues { it.isNotEmpty() }
    }
}