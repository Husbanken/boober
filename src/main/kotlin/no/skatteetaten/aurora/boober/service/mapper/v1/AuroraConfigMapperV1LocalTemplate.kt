package no.skatteetaten.aurora.boober.service.mapper.v1

import com.fasterxml.jackson.databind.JsonNode
import no.skatteetaten.aurora.boober.model.*
import no.skatteetaten.aurora.boober.service.internal.AuroraConfigException
import no.skatteetaten.aurora.boober.service.mapper.AuroraConfigFieldHandler
import no.skatteetaten.aurora.boober.service.mapper.AuroraConfigFields
import no.skatteetaten.aurora.boober.service.mapper.findExtractors
import no.skatteetaten.aurora.boober.service.openshift.OpenShiftClient

class AuroraConfigMapperV1LocalTemplate(aid: ApplicationId,
                                        auroraConfig: AuroraConfig,
                                        allFiles: List<AuroraConfigFile>,
                                        openShiftClient: OpenShiftClient) :
        AuroraConfigMapperV1(aid, auroraConfig, allFiles, openShiftClient) {

    override fun createAuroraDc(): AuroraObjectsConfig {

        val type = auroraConfigFields.extract("type", { TemplateType.valueOf(it.textValue()) })

        val templateJson = extractTemplateJson()

        return AuroraLocalTemplateConfig(
                schemaVersion = auroraConfigFields.extract("schemaVersion"),
                affiliation = auroraConfigFields.extract("affiliation"),
                cluster = auroraConfigFields.extract("cluster"),
                type = type,
                name = auroraConfigFields.extract("name"),
                envName = auroraConfigFields.extractOrDefault("envName", aid.environmentName),
                permissions = extractPermissions(),
                secrets = extractSecret(),
                config = auroraConfigFields.getConfigMap(allFiles.findExtractors("config")),
                templateJson = templateJson,
                parameters = auroraConfigFields.getParameters(allFiles.findExtractors("parameters")),
                flags = AuroraProcessConfigFlags(
                        auroraConfigFields.extract("flags/route", { it.asText() == "true" })
                ),
                fields = auroraConfigFields.fields
        )

    }

    private fun extractTemplateJson(): JsonNode {
        val templateFile = auroraConfigFields.extract("templateFile").let { fileName ->
            auroraConfig.auroraConfigFiles.find { it.name == fileName }?.contents
        }
        return templateFile ?: throw IllegalArgumentException("templateFile is required")
    }

    override fun typeValidation(fields: AuroraConfigFields): List<Exception> {
        val errors = mutableListOf<Exception>()

        val templateFile = fields.extractOrNull("templateFile")


        if (auroraConfig.auroraConfigFiles.none { it.name == templateFile }) {
            errors.add(IllegalArgumentException("The file named $templateFile does not exist in AuroraConfig"))
        }

        val secrets = extractSecret()

        if (secrets != null && secrets.isEmpty()) {
            errors.add(IllegalArgumentException("Missing secret files"))
        }

        val permissions = extractPermissions()

        permissions.admin.groups
                ?.filter { !openShiftClient.isValidGroup(it) }
                .takeIf { it != null && it.isNotEmpty() }
                ?.let { errors.add(AuroraConfigException("The following groups are not valid=${it.joinToString()}")) }

        permissions.admin.users
                ?.filter { !openShiftClient.isValidUser(it) }
                .takeIf { it != null && it.isNotEmpty() }
                ?.let { errors.add(AuroraConfigException("The following users are not valid=${it.joinToString()}")) }


        return errors

    }

    val handlers = listOf(
            AuroraConfigFieldHandler("templateFile")
    )

    override val fieldHandlers = v1Handlers + handlers + allFiles.findExtractors("parameters")
    override val auroraConfigFields = AuroraConfigFields.create(fieldHandlers, allFiles)

}