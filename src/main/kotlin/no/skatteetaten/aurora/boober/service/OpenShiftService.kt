package no.skatteetaten.aurora.boober.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.skatteetaten.aurora.boober.controller.security.UserDetailsProvider
import no.skatteetaten.aurora.boober.model.AuroraDeploy
import no.skatteetaten.aurora.boober.model.AuroraDeploymentConfig
import no.skatteetaten.aurora.boober.model.TemplateType
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.springframework.stereotype.Service
import java.io.StringWriter

@Service
class OpenShiftService(
        val userDetailsProvider: UserDetailsProvider,
        val ve: VelocityEngine,
        val mapper: ObjectMapper
) {

    fun generateBuildRequest(auroraDc: AuroraDeploymentConfig): JsonNode {
        return mergeVelocityTemplate("buildrequest.json", mapOf("adc" to auroraDc))

    }

    fun generateDeploymentRequest(auroraDc: AuroraDeploymentConfig): JsonNode {
        return mergeVelocityTemplate("deploymentrequest.json", mapOf("adc" to auroraDc))

    }
    fun generateObjects(auroraDc: AuroraDeploymentConfig): List<JsonNode> {

        val deployDescriptor = auroraDc.deployDescriptor as AuroraDeploy

        val configMap = auroraDc.config?.map { "${it.key}=${it.value}" }?.joinToString(separator = "\\n")
        val params = mapOf(
                "adc" to auroraDc,
                "configMap" to configMap,
                "dd" to deployDescriptor,
                "username" to userDetailsProvider.getAuthenticatedUser().username,
                "dockerRegistry" to "docker-registry.aurora.sits.no:5000",
                "builder" to mapOf("name" to "leveransepakkebygger", "version" to "prod"),
                "base" to mapOf("name" to "oracle8", "version" to "1")
        )

        val templatesToProcess = mutableListOf(
                "project.json",
                // It is important that the DeploymentConfig is created before the ImageStream (preferably several
                // seconds earlier - just in case), because Sprocket needs to have time to update the dc with its
                // modifications before a deployment is started. The first deployment will start as soon as the
                // ImageStream has been created, by an ImageChangeTrigger. Case in point; don't change the order of
                // these objects unless you really know whats going on.
                "deployment-config.json",
                "configmap.json",
                "service.json",
                "rolebinding.json",
                "imagestream.json"
        )

        //TODO:Hvis configmap er tom her så trenger vi ikke parse dette.

        auroraDc.secrets?.let {
            templatesToProcess.add("secret.json")
        }

        if (auroraDc.route) {
            templatesToProcess.add("route.json")
        }

        if (auroraDc.type == TemplateType.development) {
            templatesToProcess.add("build-config.json")
        }
        return templatesToProcess.map { mergeVelocityTemplate(it, params) }
    }

    private fun mergeVelocityTemplate(template: String, content: Map<String, Any?>): JsonNode {
        val context = VelocityContext()
        content.forEach { context.put(it.key, it.value) }
        val t = ve.getTemplate("templates/$template.vm")
        val sw = StringWriter()
        t.merge(context, sw)
        return mapper.readTree(sw.toString())
    }
}