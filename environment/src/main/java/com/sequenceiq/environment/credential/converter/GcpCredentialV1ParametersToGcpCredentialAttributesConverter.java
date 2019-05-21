package com.sequenceiq.environment.credential.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.GcpCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.JsonParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.P12Parameters;
import com.sequenceiq.environment.credential.attributes.gcp.GcpCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.gcp.JsonAttributes;
import com.sequenceiq.environment.credential.attributes.gcp.P12Attributes;

@Component
public class GcpCredentialV1ParametersToGcpCredentialAttributesConverter {

    public GcpCredentialAttributes convert(GcpCredentialParameters source) {
        if (source == null) {
            return null;
        }
        GcpCredentialAttributes response = new GcpCredentialAttributes();
        response.setJson(getJson(source.getJson()));
        response.setP12(getP12(source.getP12()));
        return response;
    }

    public GcpCredentialParameters convert(GcpCredentialAttributes source) {
        if (source == null) {
            return null;
        }
        GcpCredentialParameters response = new GcpCredentialParameters();
        response.setJson(getJson(source.getJson()));
        response.setP12(getP12(source.getP12()));
        return response;
    }

    private P12Attributes getP12(P12Parameters source) {
        if (source == null) {
            return null;
        }
        P12Attributes response = new P12Attributes();
        response.setProjectId(source.getProjectId());
        response.setServiceAccountId(source.getServiceAccountId());
        response.setServiceAccountPrivateKey(source.getServiceAccountPrivateKey());
        return response;
    }

    private JsonAttributes getJson(JsonParameters source) {
        if (source == null) {
            return null;
        }
        JsonAttributes response = new JsonAttributes();
        response.setCredentialJson(source.getCredentialJson());
        return response;
    }

    private P12Parameters getP12(P12Attributes source) {
        if (source == null) {
            return null;
        }
        P12Parameters response = new P12Parameters();
        response.setProjectId(source.getProjectId());
        response.setServiceAccountId(source.getServiceAccountId());
        response.setServiceAccountPrivateKey(source.getServiceAccountPrivateKey());
        return response;
    }

    private JsonParameters getJson(JsonAttributes source) {
        if (source == null) {
            return null;
        }
        JsonParameters response = new JsonParameters();
        response.setCredentialJson(source.getCredentialJson());
        return response;
    }
}
