package com.sequenceiq.environment.credential.v1.converter;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.util.NullUtil.ifNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.validation.CredentialValidator;
import com.sequenceiq.environment.credential.validation.definition.CredentialDefinitionService;

@Component
public class CredentialToCredentialV1ResponseConverter {
    private static final List<String> FIELDS_TO_COVER = Arrays.asList("password", "secretKey", "serviceAccountPrivateKey");

    private static final String PLACEHOLDER = "********";

    @Inject
    private CredentialValidator credentialValidator;

    @Inject
    private CredentialDefinitionService credentialDefinitionService;

    @Inject
    private AwsCredentialV1ParametersToAwsCredentialAttributesConverter awsConverter;

    @Inject
    private AzureCredentialV1ParametersToAzureCredentialAttributesConverter azureConverter;

    @Inject
    private CumulusCredentialV1ParametersToCumulusCredentialAttributesConverter cumulusConverter;

    @Inject
    private GcpCredentialV1ParametersToGcpCredentialAttributesConverter gcpConverter;

    @Inject
    private MockCredentialV1ParametersToMockCredentialAttributesConverter mockConverter;

    @Inject
    private OpenStackCredentialV1ParametersToOpenStackCredentialAttributesConverter openstackConverter;

    @Inject
    private YarnCredentialV1ParametersToAwsYarnAttributesConverter yarnConverter;

    @Inject
    private StringToSecretResponseConverter secretConverter;

    public CredentialResponse convert(Credential source) {
        CredentialResponse response = new CredentialResponse();
        response.setId(source.getId());
        credentialValidator.validateCredentialCloudPlatform(source.getCloudPlatform());
        response.setCloudPlatform(source.getCloudPlatform());
        response.setName(source.getName());
        if (source.getAttributes() != null) {
            convertAttributes(source, response);
            if (response.getAws() != null) {
                response.getAws().setGovCloud(source.getGovCloud());
            }
            response.setAttributes(secretConverter.convert(source.getAttributesSecret()));
            response.setResourceCrn(source.getResourceCrn());
        }
        response.setDescription(source.getDescription() == null ? "" : source.getDescription());
        return response;
    }

    private void convertAttributes(Credential source, CredentialResponse response) {
        try {
            Json json = new Json(source.getAttributes());
            credentialDefinitionService.removeSensitives(platform(source.getCloudPlatform()), json);
            coverSensitiveData(json);
            CredentialAttributes credentialAttributes = json.get(CredentialAttributes.class);
            ifNotNull(credentialAttributes.getAws(), param -> response.setAws(awsConverter.convert(param)));
            ifNotNull(credentialAttributes.getAzure(), param -> response.setAzure(azureConverter.convert(param)));
            ifNotNull(credentialAttributes.getGcp(), param -> response.setGcp(gcpConverter.convert(param)));
            ifNotNull(credentialAttributes.getOpenstack(), param -> response.setOpenstack(openstackConverter.convert(param)));
            ifNotNull(credentialAttributes.getCumulus(), param -> response.setCumulus(cumulusConverter.convert(param)));
            ifNotNull(credentialAttributes.getMock(), param -> response.setMock(mockConverter.convert(param)));
            ifNotNull(credentialAttributes.getYarn(), param -> response.setYarn(yarnConverter.convert(param)));
        } catch (IOException e) {
            throw new BadRequestException("Cannot deserialize the credential's attributes", e);
        }
    }

    public Credential convert(CredentialRequest source) {
        if (source == null) {
            return null;
        }
        Credential credential = new Credential();
        credential.setName(source.getName());
        credential.setDescription(source.getDescription());
        credential.setCloudPlatform(source.getCloudPlatform());
        convertAttributes(source, credential);
        if (source.getAws() != null) {
            credential.setGovCloud(source.getAws().getGovCloud());
        }
        return credential;
    }

    private void convertAttributes(CredentialRequest source, Credential credential) {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        ifNotNull(source.getAws(), params -> credentialAttributes.setAws(awsConverter.convert(params)));
        ifNotNull(source.getAzure(), params -> credentialAttributes.setAzure(azureConverter.convert(params)));
        ifNotNull(source.getGcp(), params -> credentialAttributes.setGcp(gcpConverter.convert(params)));
        ifNotNull(source.getCumulus(), params -> credentialAttributes.setCumulus(cumulusConverter.convert(params)));
        ifNotNull(source.getMock(), params -> credentialAttributes.setMock(mockConverter.convert(params)));
        ifNotNull(source.getOpenstack(), params -> credentialAttributes.setOpenstack(openstackConverter.convert(params)));
        ifNotNull(source.getYarn(), params -> credentialAttributes.setYarn(yarnConverter.convert(params)));
        credential.setAttributes(new Json(credentialAttributes).getValue());
    }

    private void coverSensitiveData(Json json) {
        for (String field : FIELDS_TO_COVER) {
            json.flatPaths()
                    .stream().filter(v -> v.contains(field))
                    .forEach(v -> json.replaceValue(v, PLACEHOLDER));
        }
    }
}
