package com.sequenceiq.cloudbreak.service.stack.resource.azure;

import java.util.Iterator;
import java.util.List;

import org.apache.http.Header;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;

import groovyx.net.http.HttpResponseDecorator;

public abstract class AzureResourceStatusCheckerTask implements StatusCheckerTask<AzureResourcePollerObject> {
    @Autowired
    private JsonHelper jsonHelper;

    @Override
    public boolean checkStatus(AzureResourcePollerObject t) {
        List<HttpResponseDecorator> responses = t.getResponses();
        boolean result = true;
        Iterator<HttpResponseDecorator> iterator = responses.iterator();
        while (iterator.hasNext()) {
            HttpResponseDecorator response = iterator.next();
            String requestId = getRequestId(response);
            String requestStatus = String.valueOf(t.getAzureClient().getRequestStatus(requestId));
            JsonNode jsonFromString = jsonHelper.createJsonFromString(requestStatus);
            String status = jsonFromString.get("Operation").get("Status").asText();
            if ("InProgress".equals(status)) {
                result = false;
                break;
            } else if ("Failed".equals(status)) {
                throw new InternalServerException(jsonFromString.get("Operation").get("Error").get("Message").asText());
            } else {
                iterator.remove();
            }
        }
        return result;
    }

    @Override
    public void handleTimeout(AzureResourcePollerObject t) {
        throw new InternalServerException(String.format("Operation timed out. Azure resource could not reach the desired status: %s on stack.",
                t.getStack().getId()));
    }

    @Override
    public String successMessage(AzureResourcePollerObject t) {
        MDCBuilder.buildMdcContext(t.getStack());
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
