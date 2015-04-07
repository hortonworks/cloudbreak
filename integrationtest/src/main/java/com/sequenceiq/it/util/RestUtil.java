package com.sequenceiq.it.util;

import static com.jayway.restassured.RestAssured.given;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

public class RestUtil {
    private static final Logger LOG = LoggerFactory.getLogger(RestUtil.class);

    private RestUtil() {
    }

    public static RequestSpecification createEntityRequest(String baseUri, String token, String entityJson) {
        RequestSpecification authRequestSpec = given()
                .baseUri(baseUri)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(entityJson);
        return authRequestSpec;
    }

    public static RequestSpecification entityPathRequest(String baseUri, String token, String pathParamName, String entityId) {
        RequestSpecification authRequestSpec = given()
                .baseUri(baseUri)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .pathParam(pathParamName, entityId);
        return authRequestSpec;
    }

    public static RequestSpecification entityPathRequest(String baseUri, String token) {
        RequestSpecification authRequestSpec = given()
                .baseUri(baseUri)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return authRequestSpec;
    }

    public static RequestSpecification getRequest(String baseUri, String token) {
        RequestSpecification authRequestSpec = given()
                .baseUri(baseUri)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return authRequestSpec;
    }

    public static String getResourceIdByName(String baseUri, String token, String resourcePath, String name) {
        Response resourceResponse = entityPathRequest(baseUri, token).get(resourcePath, name);
        resourceResponse.then().statusCode(HttpStatus.OK.value());
        return resourceResponse.jsonPath().getString("id");
    }

    public static RequestSpecification createAuthorizationRequest(String uaaServer, String username, String password) {
        return given()
                .baseUri(uaaServer)
                .header(HttpHeaders.ACCEPT, ContentType.URLENC.getAcceptHeader())
                .header(HttpHeaders.CONTENT_TYPE, ContentType.URLENC.toString())
                .queryParam("response_type", "token")
                .queryParam("client_id", "cloudbreak_shell")
                .queryParam("scope.0", "openid")
                .queryParam("source", "login")
                .queryParam("redirect_uri", "http://cloudbreak.shell")
                .body("credentials={\"username\": \"" + username + "\",\"password\":\"" + password + "\"}");
    }

    public static String getAccessToken(Response authResponse) throws URISyntaxException {
        return getAuthenticationDetails(authResponse).get("access_token");
    }

    public static String getToken(String identityServerAddress, String user, String password) throws Exception {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        sslContextBuilder.loadTrustMaterial(null, new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                return true;
            }
        });
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContextBuilder.build());
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();
        requestFactory.setHttpClient(httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/json");

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("credentials", String.format("{\"username\":\"%s\",\"password\":\"%s\"}", user, password));

        String token = null;
        try {
            ResponseEntity<String> authResponse = restTemplate.exchange(
                    String.format("%s/oauth/authorize?response_type=token&client_id=%s", identityServerAddress, "cloudbreak_shell"),
                    HttpMethod.POST,
                    new HttpEntity<Object>(requestBody, headers),
                    String.class
            );
            if (HttpStatus.FOUND == authResponse.getStatusCode() && authResponse.getHeaders().get("Location") != null) {
                String location = authResponse.getHeaders().get("Location").get(0);
                String[] parts = location.split("#|&|=");
                token = parts[2];
            } else {
                LOG.error("Couldn't get an access token from the identity server, check its configuration! " + "Perhaps cloudbreak_shell is not autoapproved?");
                LOG.error("Response from identity server: ");
                LOG.error("Headers: " + authResponse);
            }
        } catch (ResourceAccessException e) {
            LOG.error("Error occurred while trying to connect to identity server: " + e.getMessage());
            LOG.error("Check if your identity server is available and accepting requests on " + identityServerAddress);
        } catch (HttpClientErrorException e) {
            if (HttpStatus.UNAUTHORIZED == e.getStatusCode()) {
                LOG.error("Error occurred while getting token from identity server: " + e.getMessage());
                LOG.error("Check your username and password.");
            }
            LOG.error("Something unexpected happened, couldn't get token from identity server. Please check your configurations.");
        } catch (Exception e) {
            LOG.error("Something unexpected happened, couldn't get token from identity server. Please check your configurations.");
        }
        return token;
    }

    private static Map<String, String> getAuthenticationDetails(Response response) throws URISyntaxException {
        Map<String, String> authDetailsMap = new HashMap<String, String>();
        String detailsAsUrl = response.getHeader("Location");
        List<NameValuePair> params = URLEncodedUtils.parse(new URI(detailsAsUrl.replaceFirst("#", "?")), "UTF-8");
        for (NameValuePair nvPair : params) {
            authDetailsMap.put(nvPair.getName(), nvPair.getValue());
        }
        return authDetailsMap;
    }
}
