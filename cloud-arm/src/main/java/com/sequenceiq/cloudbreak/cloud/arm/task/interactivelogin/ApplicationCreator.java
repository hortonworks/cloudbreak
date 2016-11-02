package com.sequenceiq.cloudbreak.cloud.arm.task.interactivelogin;

import static com.sequenceiq.cloudbreak.cloud.arm.task.interactivelogin.ArmInteractiveLoginStatusCheckerTask.GRAPH_API_VERSION;
import static com.sequenceiq.cloudbreak.cloud.arm.task.interactivelogin.ArmInteractiveLoginStatusCheckerTask.GRAPH_WINDOWS;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

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
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Created by perdos on 10/18/16.
 */
@Service
public class ApplicationCreator {

    public static final int CREDENTIAL_END_YEAR = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationCreator.class);

    public String createApplication(String accessToken, String tenantId) {
        Response response = createApplicationWithGraph(accessToken, tenantId);

        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            String application = response.readEntity(String.class);
            try {
                JSONObject applicationJson = new JSONObject(application);
                String appId = applicationJson.getString("appId");
                LOGGER.info("Application created with appId: " + appId);
                return appId;
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
        } else {
            throw new IllegalStateException("Application creation error - status code: " + response.getStatus()
                    + " - error message: " + response.readEntity(String.class));
        }

    }

    private Response createApplicationWithGraph(String accessToken, String tenantId) {
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(GRAPH_WINDOWS + tenantId);
        Invocation.Builder request = resource.path("/applications").queryParam("api-version", GRAPH_API_VERSION).request();
        request.accept(MediaType.APPLICATION_JSON);

        long timeStamp = new Date().getTime();

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("availableToOtherTenants", false);
        jsonObject.addProperty("displayName", "hwx-cloud-" + timeStamp);
        jsonObject.addProperty("homepage", "http://hwx-cloud-" + timeStamp);

        JsonArray identifierUris = new JsonArray();
        identifierUris.add(new JsonPrimitive("http://hwx-cloud-" + timeStamp));
        jsonObject.add("identifierUris", identifierUris);

        JsonArray passwordCredentials = new JsonArray();
        JsonObject password = new JsonObject();
        password.addProperty("keyId", UUID.randomUUID().toString());
        password.addProperty("value", "cloudbreak");
        password.addProperty("startDate", LocalDateTime.now().minusDays(1).toString());
        password.addProperty("endDate", LocalDateTime.now().plusYears(CREDENTIAL_END_YEAR).toString());
        passwordCredentials.add(password);

        jsonObject.add("passwordCredentials", passwordCredentials);

        request.header("Authorization", "Bearer " + accessToken);
        return request.post(Entity.entity(jsonObject.toString(), MediaType.APPLICATION_JSON));
    }
}
