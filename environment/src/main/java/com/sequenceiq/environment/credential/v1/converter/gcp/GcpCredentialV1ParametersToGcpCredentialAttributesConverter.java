package com.sequenceiq.environment.credential.v1.converter.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.PROJECT_ID;
import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.GcpCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.JsonParameters;
import com.sequenceiq.environment.credential.attributes.gcp.GcpCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.gcp.JsonAttributes;

@Component
public class GcpCredentialV1ParametersToGcpCredentialAttributesConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpCredentialV1ParametersToGcpCredentialAttributesConverter.class);

    public GcpCredentialAttributes convert(GcpCredentialParameters source) {
        GcpCredentialAttributes response = new GcpCredentialAttributes();
        doIfNotNull(source.getJson(), param -> response.setJson(getJson(param)));
        return response;
    }

    public GcpCredentialParameters convert(GcpCredentialAttributes source, Json rawJson) {
        GcpCredentialParameters response = new GcpCredentialParameters();
        doIfNotNull(source.getJson(), param -> {
            JsonParameters jsonParameters = getJson(param);
            jsonParameters.setProjectId(rawJson.getString(PROJECT_ID));
            response.setJson(jsonParameters);
        });
        return response;
    }

    private JsonAttributes getJson(JsonParameters json) {
        JsonAttributes response = new JsonAttributes();
        String credentialJson = json.getCredentialJson();
        response.setCredentialJson(credentialJson);
        response.setProjectId(getProjectId(credentialJson));
        return response;
    }

    private String getProjectId(String credentialJson) {
        try {
            JsonNode jsonNode = JsonUtil.readTree(new String(Base64.decodeBase64(credentialJson.getBytes())));
            return jsonNode.get("project_id").asText();
        } catch (NullPointerException e) {
            return null;
        } catch (Exception e) {
            LOGGER.debug("project id can not be calculated from the credential: ", e);
            return null;
        }
    }

    private JsonParameters getJson(JsonAttributes json) {
        JsonParameters response = new JsonParameters();
        response.setCredentialJson(json.getCredentialJson());
        if (json.getProjectId() != null) {
            response.setProjectId(json.getProjectId());
        }
        return response;
    }
}
