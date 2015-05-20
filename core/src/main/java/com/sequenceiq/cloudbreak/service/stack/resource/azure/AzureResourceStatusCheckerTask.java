package com.sequenceiq.cloudbreak.service.stack.resource.azure;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.service.SimpleStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureResourceException;

import groovyx.net.http.HttpResponseDecorator;

public abstract class AzureResourceStatusCheckerTask extends SimpleStatusCheckerTask<AzureResourcePollerObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourceStatusCheckerTask.class);

    @Autowired
    private JsonHelper jsonHelper;

    @Override
    public boolean checkStatus(AzureResourcePollerObject t) {
        List<HttpResponseDecorator> responses = t.getResponses();
        boolean result = true;
        Iterator<HttpResponseDecorator> iterator = responses.iterator();
        while (iterator.hasNext()) {
            try {
                HttpResponseDecorator response = iterator.next();
                String requestId = getRequestId(response);
                String requestStatus = String.valueOf(t.getAzureClient().getRequestStatus(requestId));
                JsonNode jsonFromString = jsonHelper.createJsonFromString(requestStatus);
                String status = jsonFromString.get("Operation").get("Status").asText();
                if ("InProgress".equals(status)) {
                    result = false;
                    break;
                } else if ("Failed".equals(status)) {
                    String error = jsonFromString.get("Operation").get("Error").get("Message").asText();
                    throw new AzureResourceException(error);
                } else {
                    iterator.remove();
                }
            } catch (Exception ex) {
                if (ex instanceof IOException) {
                    LOGGER.warn("{}: {} happened during operation status check, the polling result will be false.", ex.getClass(), ex.getMessage());
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public void handleTimeout(AzureResourcePollerObject t) {
        throw new AzureResourceException(String.format("Operation timed out. Azure resource could not reach the desired status: %s on stack.",
                t.getStack().getId()));
    }

    @Override
    public String successMessage(AzureResourcePollerObject t) {
        return String.format("Azure resource successfully reached status: %s on stack.", t.getStack().getId());
    }

    private String getRequestId(HttpResponseDecorator response) {
        for (Header header : response.getAllHeaders()) {
            if ("x-ms-request-id".equals(header.getName())) {
                return header.getValue();
            }
        }
        return null;
    }

}
