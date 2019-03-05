package com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin;

import static com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask.AZURE_MANAGEMENT;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTenant;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTenantListResult;

@Service
public class TenantChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantChecker.class);

    public void checkTenant(String tenantId, List<AzureTenant> tenants) throws InteractiveLoginException {
        for (AzureTenant tenant : tenants) {
            if (tenant.getTenantId().equals(tenantId)) {
                LOGGER.debug("Tenant definitions successfully retrieved:" + tenant.getTenantId());
                return;
            }
        }
        throw new InteractiveLoginException("Tenant not found with id: " + tenantId);
    }

    public List<AzureTenant> getTenants(String accessToken) throws InteractiveLoginException {
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(AZURE_MANAGEMENT);
        Builder request = resource.path("/tenants")
                .queryParam("api-version", "2016-06-01")
                .request();
        request.accept(MediaType.APPLICATION_JSON);
        request.header("Authorization", "Bearer " + accessToken);
        Response response = request.get();
        return collectTenants(accessToken, response);
    }

    public List<AzureTenant> getNextSetOfTenants(String link, String accessToken) throws InteractiveLoginException {
        Client client = ClientBuilder.newClient();
        Builder request = client.target(link).request();
        request.accept(MediaType.APPLICATION_JSON);
        request.header("Authorization", "Bearer " + accessToken);
        Response response = request.get();
        return collectTenants(accessToken, response);
    }

    private List<AzureTenant> collectTenants(String accessToken, Response response) throws InteractiveLoginException {
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
            AzureTenantListResult azureTenantListResult = response.readEntity(AzureTenantListResult.class);
            List<AzureTenant> tenantList = azureTenantListResult.getValue();
            if (azureTenantListResult.getNextLink() != null) {
                tenantList.addAll(getNextSetOfTenants(azureTenantListResult.getNextLink(), accessToken));
            }
            return tenantList;
        } else {
            String errorResponse = response.readEntity(String.class);
            try {
                String errorMessage = new ObjectMapper().readTree(errorResponse).get("error").get("message").asText();
                LOGGER.info("Tenant retrieve error:" + errorMessage);
                throw new InteractiveLoginException("Error with the tenants, message: " + errorMessage);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
