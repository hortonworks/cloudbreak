package com.sequenceiq.environment.credential;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.core.Response;

import com.sequenceiq.environment.api.credential.endpoint.CredentialV1Endpoint;
import com.sequenceiq.environment.api.credential.model.request.CredentialV1Request;
import com.sequenceiq.environment.api.credential.model.response.CredentialPrerequisitesV1Response;
import com.sequenceiq.environment.api.credential.model.response.CredentialV1Response;
import com.sequenceiq.environment.api.credential.model.response.CredentialV1Responses;
import com.sequenceiq.environment.api.credential.model.response.InteractiveCredentialV1Response;

public class CredentialV1Controller implements CredentialV1Endpoint {
    @Override
    public CredentialV1Responses list() {
        return null;
    }

    @Override
    public CredentialV1Response get(String name) {
        return null;
    }

    @Override
    public CredentialV1Response post(@Valid CredentialV1Request request) {
        return null;
    }

    @Override
    public CredentialV1Response delete(String name) {
        return null;
    }

    @Override
    public CredentialV1Responses deleteMultiple(Set<String> names) {
        return null;
    }

    @Override
    public CredentialV1Response put(@Valid CredentialV1Request credentialRequest) {
        return null;
    }

    @Override
    public InteractiveCredentialV1Response interactiveLogin(@Valid CredentialV1Request credentialRequest) {
        return null;
    }

    @Override
    public CredentialPrerequisitesV1Response getPrerequisitesForCloudPlatform(String platform, String deploymentAddress) {
        return null;
    }

    @Override
    public Response initCodeGrantFlow(@Valid CredentialV1Request credentialRequest) {
        return null;
    }

    @Override
    public Response initCodeGrantFlowOnExisting(String name) {
        return null;
    }

    @Override
    public CredentialV1Response authorizeCodeGrantFlow(String platform, String code, String state) {
        return null;
    }
}
