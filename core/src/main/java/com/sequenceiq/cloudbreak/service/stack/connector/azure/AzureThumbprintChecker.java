package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class AzureThumbprintChecker extends StackBasedStatusCheckerTask<AzureThumbprintCheckerContext> {

    @Inject
    private AzureStackUtil azureStackUtil;

    @Override
    public boolean checkStatus(AzureThumbprintCheckerContext azureThumbprintCheckerContext) {
        try {
            AzureCredential credential = (AzureCredential) azureThumbprintCheckerContext.getStack().getCredential();
            AzureClient azureClient = azureStackUtil.createAzureClient(credential);
            Object virtualMachine = azureClient.getVirtualMachine(azureThumbprintCheckerContext.getProps());
            JsonNode actualObj = azureThumbprintCheckerContext.getMapper().readValue((String) virtualMachine, JsonNode.class);
            String tmpFingerPrint = actualObj.get("Deployment").get("RoleInstanceList").get("RoleInstance").get("RemoteAccessCertificateThumbprint").asText();
            if (tmpFingerPrint == null) {
                return false;
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void handleTimeout(AzureThumbprintCheckerContext azureThumbprintCheckerContext) {
        throw new AzureResourceException(String.format(
                "Could not get Azure thumbprint on '%s' machine; operation timed out.",
                azureThumbprintCheckerContext.getResource().getResourceName()));
    }

    @Override
    public String successMessage(AzureThumbprintCheckerContext azureThumbprintCheckerContext) {
        return String.format("Azure thumbprint ready on '%s' virtual machine",
                azureThumbprintCheckerContext.getResource().getResourceName());
    }
}
