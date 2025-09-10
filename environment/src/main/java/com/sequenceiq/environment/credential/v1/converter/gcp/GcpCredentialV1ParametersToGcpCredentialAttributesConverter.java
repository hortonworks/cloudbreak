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
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.P12Parameters;
import com.sequenceiq.environment.credential.attributes.gcp.GcpCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.gcp.JsonAttributes;
import com.sequenceiq.environment.credential.attributes.gcp.P12Attributes;

@Component
public class GcpCredentialV1ParametersToGcpCredentialAttributesConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpCredentialV1ParametersToGcpCredentialAttributesConverter.class);

    public GcpCredentialAttributes convert(GcpCredentialParameters source) {
        GcpCredentialAttributes response = new GcpCredentialAttributes();
        doIfNotNull(source.getJson(), param -> response.setJson(getJson(param)));
        doIfNotNull(source.getP12(), param -> response.setP12(getP12(param)));
        return response;
    }

    public GcpCredentialParameters convert(GcpCredentialAttributes source, Json rawJson) {
        GcpCredentialParameters response = new GcpCredentialParameters();
        doIfNotNull(source.getJson(), param -> {
            JsonParameters jsonParameters = getJson(param);
            jsonParameters.setProjectId(rawJson.getString(PROJECT_ID));
            response.setJson(jsonParameters);
        });
        doIfNotNull(source.getP12(), param -> {
            P12Parameters p12Parameters = getP12(param);
            p12Parameters.setProjectId(rawJson.getString(PROJECT_ID));
            response.setP12(p12Parameters);
        });
        return response;
    }

    private P12Attributes getP12(P12Parameters p12) {
        P12Attributes response = new P12Attributes();
        response.setProjectId(p12.getProjectId());
        response.setServiceAccountId(p12.getServiceAccountId());
        response.setServiceAccountPrivateKey(p12.getServiceAccountPrivateKey());
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

    private P12Parameters getP12(P12Attributes p12) {
        P12Parameters response = new P12Parameters();
        response.setProjectId(p12.getProjectId());
        response.setServiceAccountId(p12.getServiceAccountId());
        response.setServiceAccountPrivateKey(p12.getServiceAccountPrivateKey());
        return response;
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
