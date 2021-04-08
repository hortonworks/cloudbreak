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
public class RestOperationsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestOperationsService.class);

    public <T> T httpGet(URI uri, Class<T> responseClass, String token) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = getHttpHeaders(token);
            RequestEntity requestEntity = new RequestEntity(headers, HttpMethod.GET, uri);
            ResponseEntity<T> response = restTemplate.exchange(requestEntity, responseClass);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            String message = String.format("Error during http GET operation, return value %s", response);
            LOGGER.warn(message);
            throw new CloudConnectorException(message);
        } catch (HttpStatusCodeException e) {
            LOGGER.warn("Error during http operation");
            throw new CloudConnectorException("Error during http operation", e);
        }
    }

    public <T> T httpPut(URI uri, Object body, Class<T> responseClass, String token) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = getHttpHeaders(token);
            RequestEntity requestEntity = new RequestEntity(body, headers, HttpMethod.PUT, uri);
            ResponseEntity<T> response = restTemplate.exchange(requestEntity, responseClass);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            String message = String.format("Error during http PUT operation, return value %s", response);
            LOGGER.warn(message);
            throw new CloudConnectorException(message);
        } catch (HttpStatusCodeException e) {
            LOGGER.warn("Error during http operation");
            throw new CloudConnectorException("Error during http operation", e);
        }
    }

    private HttpHeaders getHttpHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        return headers;
    }

}
