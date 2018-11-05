package com.sequenceiq.cloudbreak.converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.controller.validation.credential.CredentialValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@Component
public class CredentialToCredentialResponseConverter extends AbstractConversionServiceAwareConverter<Credential, CredentialResponse> {
    private static final List<String> FIELDS_TO_COVER = Arrays.asList("password", "secretKey", "serviceAccountPrivateKey");

    private static final String PLACEHOLDER = "********";

    @Inject
    private TopologyService topologyService;

    @Inject
    private CredentialValidator credentialValidator;

    @Override
    public CredentialResponse convert(Credential source) {
        CredentialResponse credentialJson = new CredentialResponse();
        credentialJson.setId(source.getId());
        credentialValidator.validateCredentialCloudPlatform(source.cloudPlatform());
        credentialJson.setCloudPlatform(source.cloudPlatform());
        credentialJson.setName(source.getName());
        if (source.getAttributes() != null) {
            credentialJson.setParameters(Collections.singletonMap("value", source.getAttributes()));
        }
        credentialJson.setDescription(source.getDescription() == null ? "" : source.getDescription());
        if (source.getTopology() != null) {
            credentialJson.setTopologyId(source.getTopology().getId());
        }
        credentialJson.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceResponse.class));
        coverSensitiveData(credentialJson);
        return credentialJson;
    }

    private void coverSensitiveData(CredentialResponse response) {
        for (String field : FIELDS_TO_COVER) {
            if (response.getParameters().get(field) != null) {
                response.getParameters().put(field, PLACEHOLDER);
            }
        }
    }

}
