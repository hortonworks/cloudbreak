package com.sequenceiq.cloudbreak.cloud.arm.task.interactivelogin;

import static com.sequenceiq.cloudbreak.cloud.arm.task.interactivelogin.ArmInteractiveLoginStatusCheckerTask.GRAPH_API_VERSION;
import static com.sequenceiq.cloudbreak.cloud.arm.task.interactivelogin.ArmInteractiveLoginStatusCheckerTask.GRAPH_WINDOWS;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;

/**
 * Created by perdos on 10/18/16.
 */
@Service
public class PrincipalCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrincipalCreator.class);

    @Retryable(value = IllegalStateException.class, maxAttempts = 10, backoff = @Backoff(delay = 1000))
    public String createServicePrincipal(String accessToken, String appId, String tenantId) {
        Response response = createServicePrincipalWithGraph(accessToken, appId, tenantId);

        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            String principal = response.readEntity(String.class);

            try {
                JSONObject principalJson = new JSONObject(principal);
                String objectId = principalJson.getString("objectId");
                LOGGER.info("Service principal created with objectId: " + objectId);
                return objectId;
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
        } else {
            throw new IllegalStateException("Service principal creation error - status code: " + response.getStatus()
                    + " - error message: " + response.readEntity(String.class));
        }
    }

    private Response createServicePrincipalWithGraph(String accessToken, String appId, String tenantId) {
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(GRAPH_WINDOWS + tenantId);
        Invocation.Builder request = resource.path("servicePrincipals").queryParam("api-version", GRAPH_API_VERSION).request();
        request.accept(MediaType.APPLICATION_JSON);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("appId", appId);
        jsonObject.addProperty("accountEnabled", true);

        request.header("Authorization", "Bearer " + accessToken);
        return request.post(Entity.entity(jsonObject.toString(), MediaType.APPLICATION_JSON));
    }
}
