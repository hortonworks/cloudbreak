package com.sequenceiq.environment.credential.v1.converter.gcp;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

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
        GcpCredentialAttributes response = new GcpCredentialAttributes();
        doIfNotNull(source.getJson(), param -> response.setJson(getJson(param)));
        doIfNotNull(source.getP12(), param -> response.setP12(getP12(param)));
        return response;
    }

    public GcpCredentialParameters convert(GcpCredentialAttributes source) {
        GcpCredentialParameters response = new GcpCredentialParameters();
        doIfNotNull(source.getJson(), param -> response.setJson(getJson(param)));
        doIfNotNull(source.getP12(), param -> response.setP12(getP12(param)));
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
