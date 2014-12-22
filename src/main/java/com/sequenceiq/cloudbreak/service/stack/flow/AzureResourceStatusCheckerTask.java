package com.sequenceiq.cloudbreak.service.stack.flow;

import org.apache.http.Header;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StackDependentStatusCheckerTask;

@Component
public class AzureResourceStatusCheckerTask extends StackDependentStatusCheckerTask<AzureResourcePollerObject> {

    @Autowired
    private JsonHelper jsonHelper;

    @Override
    public boolean checkStatus(AzureResourcePollerObject t) {
        String requestId = getRequestId(t);
        String requestStatus = String.valueOf(t.getAzureClient().getRequestStatus(requestId));
        JsonNode jsonFromString = jsonHelper.createJsonFromString(requestStatus);
        if (!"InProgress".equals(jsonFromString.get("Operation").get("Status").asText())) {
            return true;
        }
        return false;
    }

    private String getRequestId(AzureResourcePollerObject t) {
        for (Header header : t.getHttpResponseDecorator().getAllHeaders()) {
            if ("x-ms-request-id".equals(header.getName())) {
                return header.getValue();
            }
        }
        return null;
    }

    @Override
    public void handleTimeout(AzureResourcePollerObject t) {
        throw new InternalServerException(String.format("Azure instances could not reach the desired status: %s on stack.", t.getStack().getId()));
    }

    @Override
    public String successMessage(AzureResourcePollerObject t) {
        MDCBuilder.buildMdcContext(t.getStack());
        return String.format("Azure instances successfully reached status: %s on stack.", t.getStack().getId());
    }

    @Override
    public void handleExit(AzureResourcePollerObject t) {
        return;
    }
}
