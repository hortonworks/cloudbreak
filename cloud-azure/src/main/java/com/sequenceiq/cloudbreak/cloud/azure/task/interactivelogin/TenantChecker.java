package com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin;

import static com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask.AZURE_MANAGEMENT;

import java.io.IOException;
import java.util.ArrayList;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTenant;

@Service
public class TenantChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantChecker.class);

    public void checkTenant(String tenantId, String accessToken) throws InteractiveLoginException {
        if (tenantId == null) {
            throw new InteractiveLoginException("Parameter tenantId is required and cannot be null.");
        }
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(AZURE_MANAGEMENT);
        Builder request = resource.path("/tenants")
                .queryParam("api-version", "2016-06-01")
                .request();
        request.accept(MediaType.APPLICATION_JSON);

        request.header("Authorization", "Bearer " + accessToken);
        Response response = request.get();

        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
            String entity = response.readEntity(String.class);
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode tenantArray = mapper.readTree(entity).get("value");
                ObjectReader reader = mapper.readerFor(new TypeReference<ArrayList<AzureTenant>>() {
                });

                List<AzureTenant> tenants = reader.readValue(tenantArray);
                for (AzureTenant tenant: tenants) {
                    if (tenant.getTenantId().equals(tenantId)) {
                        LOGGER.debug("Tenant definitions successfully retrieved:" + tenant.getTenantId());
                        return;
                    }
                }
            } catch (IOException e) {
                throw new InteractiveLoginException(e.toString());
            }
            throw new InteractiveLoginException("Tenant specified in Profile file not found with id: " + tenantId);
        } else {
            String errorResponse = response.readEntity(String.class);
            try {
                String errorMessage = new ObjectMapper().readTree(errorResponse).get("error").get("message").asText();
                LOGGER.error("Tenant retrieve error:" + errorMessage);
                throw new InteractiveLoginException("Error with the tenant specified in Profile file id: " + tenantId + ", message: " + errorMessage);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
