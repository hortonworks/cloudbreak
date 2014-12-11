package com.sequenceiq.cloudbreak.it;

import static com.jayway.restassured.RestAssured.given;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

public class IntegrationTestUtil {

    private IntegrationTestUtil() {
    }

    public static RequestSpecification createAuthorizationRequest(String username, String password) {
        RequestSpecification authRequestSpec = given()
                .header(HttpHeaders.ACCEPT, ContentType.URLENC.getAcceptHeader())
                .header(HttpHeaders.CONTENT_TYPE, ContentType.URLENC.toString())
                .queryParam("response_type", "token")
                .queryParam("client_id", "cloudbreak_shell")
                .queryParam("scope.0", "openid")
                .queryParam("source", "login")
                .queryParam("redirect_uri", "http://cloudbreak.shell")
                .body("credentials={\"username\": \"" + username + "\",\"password\":\"" + password + "\"}");
        return authRequestSpec;
    }

    public static RequestSpecification createEntityRequest(String token, String entityJson) {
        RequestSpecification authRequestSpec = given()
                .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(entityJson);
        return authRequestSpec;
    }

    public static RequestSpecification entityPathRequest(String token, String pathParamName, String entityId) {
        RequestSpecification authRequestSpec = given().
                header(HttpHeaders.CONTENT_TYPE, ContentType.JSON.toString()).
                header(HttpHeaders.AUTHORIZATION, "Bearer " + token).
                pathParam(pathParamName, entityId);
        return authRequestSpec;
    }

    public static RequestSpecification getRequest(String token) {
        RequestSpecification authRequestSpec = given().
                header(HttpHeaders.CONTENT_TYPE, ContentType.JSON.toString()).
                header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return authRequestSpec;
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

    public static String getAccessToken(Response authResponse) throws URISyntaxException {
        return getAuthenticationDetails(authResponse).get("access_token");
    }

}
