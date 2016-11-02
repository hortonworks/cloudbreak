package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.stack.resource.definition.credential.CredentialDefinitionService;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@Component
public class CredentialToJsonConverter extends AbstractConversionServiceAwareConverter<Credential, CredentialResponse> {
    private static final String PASSWORD_FIELD = "password";

    private static final String PASSWORD_PLACEHOLDER = "********";

    @Inject
    private CredentialDefinitionService credentialDefinitionService;

    @Inject
    private TopologyService topologyService;

    @Override
    public CredentialResponse convert(Credential source) {
        CredentialResponse credentialJson = new CredentialResponse();
        credentialJson.setId(source.getId());
        credentialJson.setCloudPlatform(source.cloudPlatform());
        credentialJson.setName(source.getName());
        credentialJson.setPublicInAccount(source.isPublicInAccount());
        if (source.getAttributes() != null) {
            Map<String, Object> parameters = credentialDefinitionService.revertProperties(platform(source.cloudPlatform()), source.getAttributes().getMap());
            credentialJson.setParameters(parameters);
        }
        credentialJson.setDescription(source.getDescription() == null ? "" : source.getDescription());
        credentialJson.setPublicKey(source.getPublicKey());
        credentialJson.setLoginUserName(source.getLoginUserName());
        if (source.getTopology() != null) {
            credentialJson.setTopologyId(source.getTopology().getId());
        }
        clearPasswordField(credentialJson);
        return credentialJson;
    }

    private void clearPasswordField(CredentialResponse response) {
        if (response.getParameters().get(PASSWORD_FIELD) != null) {
            response.getParameters().put(PASSWORD_FIELD, PASSWORD_PLACEHOLDER);
        }
    }
}
