package no.skatteetaten.aurora.boober.controller

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

import com.fasterxml.jackson.databind.ObjectMapper

import groovy.json.JsonSlurper
import no.skatteetaten.aurora.boober.controller.security.User
import no.skatteetaten.aurora.boober.service.openshift.OpenShiftClient
import no.skatteetaten.aurora.boober.utils.SampleFilesCollector

class SetupControllerTest extends AbstractControllerTest {

  public static final String AFFILIATION = "aos"
  public static final String ENV_NAME = "booberdev"
  public static final String APP_NAME = "verify-ebs-users"

  @Autowired
  OpenShiftClient openShiftClient

  @Autowired
  ObjectMapper mapper

  def "Should fail when Aurora Config contains errors"() {
    given:
      def files = SampleFilesCollector.qaEbsUsersSampleFiles
      files.put("about.json", mapper.readTree("{}"))
      SetupCommand cmd = new SetupCommand(AFFILIATION, new AuroraConfigPayload(files, [:]), new SetupParamsPayload([ENV_NAME], [APP_NAME], [:], false))
      def json = mapper.writeValueAsString(cmd)

    when:
      def response = mockMvc
          .perform(put("/affiliation/${AFFILIATION}/setup")
          .with(user(new User("test", "test", "Test User")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(json))
      def body = new JsonSlurper().parseText(response.andReturn().response.getContentAsString())

    then:
      body.items[0]["messages"].size() == 2
      response.andExpect(status().is4xxClientError())
  }
}
