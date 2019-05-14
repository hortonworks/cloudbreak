package com.sequenceiq.environment.credential.converter;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.credential.model.response.CredentialV1Response;
import com.sequenceiq.environment.credential.Credential;
import com.sequenceiq.environment.credential.definition.CredentialDefinitionService;
import com.sequenceiq.environment.credential.validator.CredentialValidator;

@Component
public class CredentialToCredentialV1ResponseConverter extends AbstractConversionServiceAwareConverter<Credential, CredentialV1Response> {
    private static final List<String> FIELDS_TO_COVER = Arrays.asList("password", "secretKey", "serviceAccountPrivateKey");

    private static final String PLACEHOLDER = "********";

    @Inject
    private CredentialValidator credentialValidator;

    @Inject
    private CredentialDefinitionService credentialDefinitionService;

    @Override
    public CredentialV1Response convert(Credential source) {
        CredentialV1Response response = new CredentialV1Response();
        response.setId(source.getId());
        credentialValidator.validateCredentialCloudPlatform(source.getCloudPlatform());
        response.setCloudPlatform(source.getCloudPlatform());
        response.setName(source.getName());
//        if (source.getAttributes() != null) {
//            Json secretAttributes = new Json(source.getAttributes());
//            Map<String, Object> parameters = credentialDefinitionService.removeSensitives(platform(source.getCloudPlatform()), secretAttributes.getMap());
//            convertValuesToBooleanIfNecessary(parameters);
//            coverSensitiveData(parameters);
//            credentialParameterSetterUtil.setProperParameters(source.getCloudPlatform(), response, parameters);
//            if (response.getAws() != null) {
//                response.getAws().setGovCloud(source.getGovCloud());
//            }
//            response.setAttributes(getConversionService().convert(source.getAttributesSecret(), SecretV4Response.class));
//        }
        response.setDescription(source.getDescription() == null ? "" : source.getDescription());
        return response;
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
