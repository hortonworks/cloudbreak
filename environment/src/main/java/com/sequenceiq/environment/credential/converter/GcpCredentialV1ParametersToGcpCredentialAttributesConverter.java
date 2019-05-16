package com.sequenceiq.environment.credential.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.credential.model.parameters.gcp.GcpCredentialV1Parameters;
import com.sequenceiq.environment.api.credential.model.parameters.gcp.JsonParameters;
import com.sequenceiq.environment.api.credential.model.parameters.gcp.P12Parameters;
import com.sequenceiq.environment.credential.attributes.gcp.GcpCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.gcp.JsonAttributes;
import com.sequenceiq.environment.credential.attributes.gcp.P12Attributes;

@Component
public class GcpCredentialV1ParametersToGcpCredentialAttributesConverter {

    public GcpCredentialAttributes convert(GcpCredentialV1Parameters source) {
        GcpCredentialAttributes response = new GcpCredentialAttributes();
        response.setJson(getJson(source.getJson()));
        response.setP12(getP12(source.getP12()));
        return response;
    }

    public GcpCredentialV1Parameters convert(GcpCredentialAttributes source) {
        GcpCredentialV1Parameters response = new GcpCredentialV1Parameters();
        response.setJson(getJson(source.getJson()));
        response.setP12(getP12(source.getP12()));
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
        response.setCredentialJson(json.getCredentialJson());
        return response;
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
        return response;
    }
}
