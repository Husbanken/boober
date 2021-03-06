package no.skatteetaten.aurora.boober.service.openshift

import com.fasterxml.jackson.databind.JsonNode
import no.skatteetaten.aurora.boober.service.OpenShiftException
import no.skatteetaten.aurora.boober.service.openshift.token.TokenProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import java.net.URI

open class OpenShiftResourceClient(@Value("\${openshift.url}") val baseUrl: String,
                                   val tokenProvider: TokenProvider,
                                   val openShiftRequestHandler: OpenShiftRequestHandler) {

    fun put(kind: String, namespace: String, name: String, payload: JsonNode): ResponseEntity<JsonNode> {
        val urls: OpenShiftApiUrls = OpenShiftApiUrls.createOpenShiftApiUrls(baseUrl, kind, namespace, name)
        val headers: HttpHeaders = getAuthorizationHeaders()
        return openShiftRequestHandler.exchange(RequestEntity<JsonNode>(payload, headers, HttpMethod.PUT, URI(urls.update)))
    }

    open fun get(kind: String, namespace: String, name: String, retry: Boolean = true): ResponseEntity<JsonNode>? {

        val urls: OpenShiftApiUrls = OpenShiftApiUrls.createOpenShiftApiUrls(baseUrl, kind, namespace, name)
        val url = urls.get ?: return null

        return get(url, retry = retry)
    }

    open fun get(url: String, headers: HttpHeaders = getAuthorizationHeaders(), retry: Boolean = true): ResponseEntity<JsonNode>? {
        try {
            return openShiftRequestHandler.exchange(RequestEntity<Any>(headers, HttpMethod.GET, URI(url)), retry)
        } catch (e: OpenShiftException) {
            if (e.cause is HttpClientErrorException && e.cause.statusCode == HttpStatus.NOT_FOUND) {
                return null
            }
            throw e
        }
    }

    open fun post(kind: String, namespace: String, name: String? = null, payload: JsonNode): ResponseEntity<JsonNode> {

        val urls: OpenShiftApiUrls = OpenShiftApiUrls.createOpenShiftApiUrls(baseUrl, kind, namespace, name)
        val headers: HttpHeaders = getAuthorizationHeaders()
        return openShiftRequestHandler.exchange(RequestEntity<JsonNode>(payload, headers, HttpMethod.POST, URI(urls.create)))
    }

    fun delete(kind: String, namespace: String, name: String? = null): ResponseEntity<JsonNode> {

        val urls: OpenShiftApiUrls = OpenShiftApiUrls.createOpenShiftApiUrls(baseUrl, kind, namespace, name)
        val headers: HttpHeaders = getAuthorizationHeaders()
        return openShiftRequestHandler.exchange(RequestEntity<JsonNode>(headers, HttpMethod.DELETE, URI(urls.get)))
    }

    open fun getAuthorizationHeaders(): HttpHeaders {
        return createHeaders(tokenProvider.getToken())
    }

    fun createHeaders(token: String): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("Authorization", "Bearer " + token)
        return headers
    }
}