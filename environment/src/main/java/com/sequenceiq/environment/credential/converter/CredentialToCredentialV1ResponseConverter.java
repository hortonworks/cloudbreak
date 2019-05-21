package com.sequenceiq.environment.credential.converter;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

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
        if (source == null) {
            return null;
        }
        CredentialResponse response = new CredentialResponse();
        response.setId(source.getId());
        credentialValidator.validateCredentialCloudPlatform(source.getCloudPlatform());
        response.setCloudPlatform(source.getCloudPlatform());
        response.setName(source.getName());
        if (source.getAttributes() != null) {
            try {
                Json json = new Json(source.getAttributes());
                credentialDefinitionService.removeSensitives(platform(source.getCloudPlatform()), json);
                coverSensitiveData(json);
                CredentialAttributes credentialAttributes = json.get(CredentialAttributes.class);
                response.setAzure(azureConverter.convert(credentialAttributes.getAzure()));
                response.setAws(awsConverter.convert(credentialAttributes.getAws()));
                response.setCumulus(cumulusConverter.convert(credentialAttributes.getCumulus()));
                response.setGcp(gcpConverter.convert(credentialAttributes.getGcp()));
                response.setMock(mockConverter.convert(credentialAttributes.getMock()));
                response.setOpenstack(openstackConverter.convert(credentialAttributes.getOpenstack()));
                response.setYarn(yarnConverter.convert(credentialAttributes.getYarn()));
            } catch (IOException e) {
                throw new BadRequestException("Cannot deserialize the credential's attributes", e);
            }

            if (response.getAws() != null) {
                response.getAws().setGovCloud(source.getGovCloud());
            }
            response.setAttributes(secretConverter.convert(source.getAttributesSecret()));
            response.setResourceCrn(source.getResourceCrn());
        }
        response.setDescription(source.getDescription() == null ? "" : source.getDescription());
        return response;
    }

    public Credential convert(CredentialRequest source) {
        if (source == null) {
            return null;
        }
        Credential credential = new Credential();
        credential.setName(source.getName());
        credential.setDescription(source.getDescription());
        credential.setCloudPlatform(source.getCloudPlatform());
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        credentialAttributes.setAws(awsConverter.convert(source.getAws()));
        credentialAttributes.setAzure(azureConverter.convert(source.getAzure()));
        credentialAttributes.setGcp(gcpConverter.convert(source.getGcp()));
        credentialAttributes.setCumulus(cumulusConverter.convert(source.getCumulus()));
        credentialAttributes.setMock(mockConverter.convert(source.getMock()));
        credentialAttributes.setOpenstack(openstackConverter.convert(source.getOpenstack()));
        credentialAttributes.setYarn(yarnConverter.convert(source.getYarn()));
        credential.setAttributes(new Json(credentialAttributes).getValue());
        if (source.getAws() != null) {
            credential.setGovCloud(source.getAws().getGovCloud());
        }
        return credential;
    }

    private void coverSensitiveData(Json json) {
        for (String field : FIELDS_TO_COVER) {
            json.flatPaths()
                    .stream().filter(v -> v.contains(field))
                    .forEach(v -> json.replaceValue(v, PLACEHOLDER));
        }
    }
}
