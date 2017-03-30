package no.skatteetaten.aurora.boober.service

import com.fasterxml.jackson.databind.JsonNode
import no.skatteetaten.aurora.boober.model.AuroraConfig
import no.skatteetaten.aurora.boober.model.AuroraDeploymentConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class ApplicationId(
        val environmentName: String,
        val applicationName: String
)

data class ApplicationResult(
        val applicationId: ApplicationId,
        val auroraDc: AuroraDeploymentConfig,
        val openShiftResponses: List<OpenShiftResponse>
)

@Service
class SetupService(
        val auroraConfigParserService: AuroraConfigParserService,
        val openShiftService: OpenShiftService,
        val openShiftClient: OpenShiftClient) {

    val logger: Logger = LoggerFactory.getLogger(SetupService::class.java)

    fun executeSetup(token: String, auroraConfig: AuroraConfig, environmentName: String, applicationName: String): List<ApplicationResult> {

        val auroraDc: AuroraDeploymentConfig = auroraConfigParserService.createAuroraDcFromAuroraConfig(auroraConfig, environmentName, applicationName)

        logger.info("Creating OpenShift objects for application ${auroraDc.name} in namespace ${auroraDc.namespace}")
        val openShiftObjects: List<JsonNode> = openShiftService.generateObjects(auroraDc, token)
        val openShiftResponses: List<OpenShiftResponse> = openShiftClient.applyMany(auroraDc.namespace, openShiftObjects, token)

        val applicationResults = mutableListOf<ApplicationResult>()
        applicationResults.add(ApplicationResult(
                applicationId = ApplicationId(environmentName, applicationName),
                auroraDc = auroraDc,
                openShiftResponses = openShiftResponses
        ))
        return applicationResults
    }
}