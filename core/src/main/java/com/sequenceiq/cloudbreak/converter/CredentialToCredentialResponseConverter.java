package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.controller.validation.credential.CredentialValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.stack.resource.definition.credential.CredentialDefinitionService;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@Component
public class CredentialToCredentialResponseConverter extends AbstractConversionServiceAwareConverter<Credential, CredentialResponse> {
    private static final List<String> FIELDS_TO_COVER = Arrays.asList("password", "secretKey", "serviceAccountPrivateKey");

    private static final String PLACEHOLDER = "********";

    @Inject
    private CredentialDefinitionService credentialDefinitionService;

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
            Map<String, Object> parameters =
                    credentialDefinitionService.revertAndRemoveProperties(platform(source.cloudPlatform()), source.getAttributes().getMap());
            convertValuesToBooleanIfNecessary(parameters);
            credentialJson.setParameters(parameters);
        }
        credentialJson.setDescription(source.getDescription() == null ? "" : source.getDescription());
        if (source.getTopology() != null) {
            credentialJson.setTopologyId(source.getTopology().getId());
        }
        credentialJson.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceResponse.class));
        coverSensitiveData(credentialJson);
        credentialJson.setGovCloud(source.getGovCloud());
        return credentialJson;
    }

    private void coverSensitiveData(CredentialResponse response) {
        for (String field : FIELDS_TO_COVER) {
            if (response.getParameters().get(field) != null) {
                response.getParameters().put(field, PLACEHOLDER);
            }
        }
    }

    private void convertValuesToBooleanIfNecessary(Map<String, Object> parameters) {
        parameters.keySet().forEach(s -> {
            if (isStringAndBoolean(parameters.get(s))) {
                parameters.put(s, Boolean.parseBoolean((String) parameters.get(s)));
            }
        });
    }

    private boolean isStringAndBoolean(Object o) {
        return o instanceof String && (TRUE.toString().equalsIgnoreCase((String) o) || FALSE.toString().equalsIgnoreCase((String) o));
    }
}
