package com.sequenceiq.cloudbreak.cloud.azure.rest;

import java.net.URI;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class AzureRestOperationsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureRestOperationsService.class);

    @Inject
    private RestTemplateFactory restTemplateFactory;

    public <T> T httpGet(URI uri, Class<T> responseClass, String token) {
        RestTemplate restTemplate = restTemplateFactory.create();
        HttpHeaders headers = getHttpHeaders(token);
        RequestEntity requestEntity = new RequestEntity(headers, HttpMethod.GET, uri);
        return executeHttpCall(responseClass, restTemplate, requestEntity, HttpMethod.GET);
    }

    public <T> T httpPut(URI uri, Object body, Class<T> responseClass, String token) {
        RestTemplate restTemplate = restTemplateFactory.create();
        HttpHeaders headers = getHttpHeaders(token);
        RequestEntity requestEntity = new RequestEntity(body, headers, HttpMethod.PUT, uri);
        return executeHttpCall(responseClass, restTemplate, requestEntity, HttpMethod.PUT);
    }

    private <T> T executeHttpCall(Class<T> responseClass, RestTemplate restTemplate, RequestEntity requestEntity, HttpMethod httpMethod) {
        try {
            ResponseEntity<T> response = restTemplate.exchange(requestEntity, responseClass);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            String message = String.format("Error during http %s operation, return value %s", httpMethod.toString(), response);
            LOGGER.warn(message);
            throw new AzureRestResponseException(message);
        } catch (HttpStatusCodeException e) {
            String message = String.format("Error during http %s operation: %s", httpMethod.toString(), e.getMessage());
            LOGGER.warn(message, e);
            throw new AzureRestResponseException(message, e);
        }
    }

    private HttpHeaders getHttpHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.add("CommandName", "vm image terms accept");
//        headers.add("ParameterSetName", "--publisher --offer --plan");
//        headers.add("User-Agent", "AZURECLI/2.44.1 (DEB) azsdk-python-mgmt-marketplaceordering/1.1.0 Python/3.10.8 (Linux-5.15.0-67-generic-x86_64-with-glibc2.31)");
        return headers;
    }

}