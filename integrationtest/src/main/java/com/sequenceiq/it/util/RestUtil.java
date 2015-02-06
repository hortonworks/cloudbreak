package com.sequenceiq.it.util;

import static com.jayway.restassured.RestAssured.given;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.http.HttpStatus;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

public class RestUtil {
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
