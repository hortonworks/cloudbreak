package com.sequenceiq.cloudbreak.converter.v4.credentials;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.controller.validation.credential.CredentialValidator;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.credential.CredentialParameterSetterUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.definition.credential.CredentialDefinitionService;
import com.sequenceiq.secret.model.SecretResponse;

@Component
public class CredentialToCredentialV4ResponseConverter extends AbstractConversionServiceAwareConverter<Credential, CredentialV4Response> {
    private static final List<String> FIELDS_TO_COVER = Arrays.asList("password", "secretKey", "serviceAccountPrivateKey");

    private static final String PLACEHOLDER = "********";

    @Inject
    private CredentialValidator credentialValidator;

    @Inject
    private CredentialDefinitionService credentialDefinitionService;

    @Inject
    private CredentialParameterSetterUtil credentialParameterSetterUtil;

    @Override
    public CredentialV4Response convert(Credential source) {
        CredentialV4Response credentialJson = new CredentialV4Response();
        credentialJson.setId(source.getId());
        credentialValidator.validateCredentialCloudPlatform(source.cloudPlatform());
        credentialJson.setCloudPlatform(source.cloudPlatform());
        credentialJson.setName(source.getName());
        if (source.getAttributes() != null) {
            Json secretAttributes = new Json(source.getAttributes());
            Map<String, Object> parameters = credentialDefinitionService.removeSensitives(platform(source.cloudPlatform()), secretAttributes.getMap());
            convertValuesToBooleanIfNecessary(parameters);
            coverSensitiveData(parameters);
            credentialParameterSetterUtil.setProperParameters(source.cloudPlatform(), credentialJson, parameters);
            if (credentialJson.getAws() != null) {
                credentialJson.getAws().setGovCloud(source.getGovCloud());
            }
            credentialJson.setAttributes(getConversionService().convert(source.getAttributesSecret(), SecretResponse.class));
        }
        credentialJson.setDescription(source.getDescription() == null ? "" : source.getDescription());
        credentialJson.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceV4Response.class));
        return credentialJson;
    }

    private void coverSensitiveData(Map<String, Object> params) {
        for (String field : FIELDS_TO_COVER) {
            if (params.get(field) != null) {
                params.put(field, PLACEHOLDER);
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
