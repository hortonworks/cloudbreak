package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@Service
public class AzureRestOperationsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureRestOperationsService.class);

    public <T> T httpGet(URI uri, Class<T> responseClass, String token) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders(token);
        RequestEntity requestEntity = new RequestEntity(headers, HttpMethod.GET, uri);
        return executeHttpCall(responseClass, restTemplate, requestEntity, HttpMethod.GET);
    }

    public <T> T httpPut(URI uri, Object body, Class<T> responseClass, String token) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders(token);
        RequestEntity requestEntity = new RequestEntity(body, headers, HttpMethod.PUT, uri);
        return executeHttpCall(responseClass, restTemplate, requestEntity, HttpMethod.GET);
    }

    private <T> T executeHttpCall(Class<T> responseClass, RestTemplate restTemplate, RequestEntity requestEntity, HttpMethod httpMethod) {
        try {
            ResponseEntity<T> response = restTemplate.exchange(requestEntity, responseClass);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            String message = String.format("Error during http %s operation, return value %s", httpMethod.toString(), response);
            LOGGER.warn(message);
            throw new CloudConnectorException(message);
        } catch (HttpStatusCodeException e) {
            String message = String.format("Error during http %s operation", httpMethod.toString());
            LOGGER.warn(message);
            throw new CloudConnectorException(message, e);
        }
    }

    private HttpHeaders getHttpHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        return headers;
    }

}
