package no.skatteetaten.aurora.boober.service

import static no.skatteetaten.aurora.boober.model.ApplicationId.aid

import groovy.json.JsonSlurper
import no.skatteetaten.aurora.boober.model.AuroraDeploymentSpec

class OpenShiftObjectGeneratorImageStreamTest extends AbstractOpenShiftObjectGeneratorTest {

  OpenShiftObjectGenerator objectGenerator = createObjectGenerator()

  def "Verify labels are created properly"() {

    given:
      def auroraConfigJson = [
          "about.json"         : DEFAULT_ABOUT,
          "utv/about.json"     : DEFAULT_UTV_ABOUT,
          "aos-simple.json"    : AOS_SIMPLE_JSON,
          "utv/aos-simple.json": '''{ "version": "SNAPSHOT-feature_MFU_3056-20171122.091423-23-b2.2.5-oracle8-1.4.0" }'''
      ]

      AuroraDeploymentSpec deploymentSpec = createDeploymentSpec(auroraConfigJson, aid("utv", "aos-simple"))

    when:
      def imageStream = objectGenerator.generateImageStream('deploy-id', deploymentSpec)
      imageStream = new JsonSlurper().parseText(imageStream.toString())

    then:
      def labels = imageStream.metadata.labels
      labels.affiliation == AFFILIATION
      labels.app == 'aos-simple'
      labels.booberDeployId == 'deploy-id'
      labels.releasedVersion == 'APSHOT-feature_MFU_3056-20171122.091423-23-b2.2.5-oracle8-1.4.0'
      labels.updatedBy == 'aurora'
      labels.each { name, value -> assert value.length() <= OpenShiftObjectLabelService.MAX_LABEL_VALUE_LENGTH }
  }
}
