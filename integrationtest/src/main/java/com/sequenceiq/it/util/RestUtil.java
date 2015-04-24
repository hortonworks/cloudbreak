package com.sequenceiq.it.util;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

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

public class RestUtil {
    private static final Logger LOG = LoggerFactory.getLogger(RestUtil.class);

    private RestUtil() {
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
}
