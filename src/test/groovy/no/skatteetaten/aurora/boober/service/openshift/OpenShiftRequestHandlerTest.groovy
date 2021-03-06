package no.skatteetaten.aurora.boober.service.openshift

import static org.springframework.http.HttpMethod.GET
import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.test.web.client.MockRestServiceServer

import com.fasterxml.jackson.databind.JsonNode

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import groovy.json.JsonOutput
import no.skatteetaten.aurora.boober.service.AbstractAuroraDeploymentSpecSpringTest
import no.skatteetaten.aurora.boober.service.OpenShiftException

class OpenShiftRequestHandlerTest extends AbstractAuroraDeploymentSpecSpringTest {

  @Value('${openshift.url}')
  String openShiftUrl

  @Autowired
  MockRestServiceServer osClusterMock

  @Autowired
  OpenShiftRequestHandler requestHandler

  @Value('${openshift.url}/oapi/v1/namespaces/aos/deploymentconfigs/webleveranse')
  String resourceUrl

  def setup() {
    Logger root = (Logger) LoggerFactory.getLogger("no.skatteetaten")
    root.setLevel(Level.DEBUG)
  }

  def "Succeeds even if the request fails a couple of times"() {

    given:
      def resource = loadResource(ResourceMergerTest.simpleName, "dc-webleveranse.json")
      2.times { osClusterMock.expect(requestTo(resourceUrl)).andRespond(withBadRequest()) }
      osClusterMock.expect(requestTo(resourceUrl)).andRespond(withSuccess(resource, APPLICATION_JSON))

    when:
      ResponseEntity<JsonNode> entity = requestHandler.
          exchange(new RequestEntity<Object>(GET, new URI(resourceUrl)), true)

    then:
      JsonOutput.prettyPrint(entity.body.toString()) == JsonOutput.prettyPrint(resource)
  }

  def "Fails when exceeds retry attempts"() {

    given:
      def resourceUrl = "$openShiftUrl/oapi/v1/namespaces/aos/deploymentconfigs/webleveranse"
      3.times { osClusterMock.expect(requestTo(resourceUrl)).andRespond(withBadRequest()) }

    when:
      requestHandler.exchange(new RequestEntity<Object>(GET, new URI(resourceUrl)), true)

    then:
      thrown(OpenShiftException)
  }

  def "Fails immediately when retry is disabled"() {

    given:
      def resourceUrl = "$openShiftUrl/oapi/v1/namespaces/aos/deploymentconfigs/webleveranse"
      1.times { osClusterMock.expect(requestTo(resourceUrl)).andRespond(withBadRequest()) }
      0 * osClusterMock._

    when:
      requestHandler.exchange(new RequestEntity<Object>(GET, new URI(resourceUrl)), false)

    then:
      thrown(OpenShiftException)
  }

  def "Get token snippet from auth header"() {
    expect:
      RetryLogger.getTokenSnippetFromAuthHeader(new HttpHeaders().with {
        it.add(org.springframework.http.HttpHeaders.AUTHORIZATION, "Authorization $token" as String)
        it
      }) == snippet

    where:
      token | snippet
      "some_long_token" | "some_"
  }
}
