package com.sequenceiq.environment.credential.converter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.api.credential.model.request.CredentialV1Request;
import com.sequenceiq.environment.api.credential.model.response.CredentialV1Response;
import com.sequenceiq.environment.credential.Credential;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.definition.CredentialDefinitionService;
import com.sequenceiq.environment.credential.validator.CredentialValidator;
import com.sequenceiq.secret.model.StringToSecretResponseConverter;

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

    public CredentialV1Response convert(Credential source) {
        CredentialV1Response response = new CredentialV1Response();
        response.setId(source.getId());
        credentialValidator.validateCredentialCloudPlatform(source.getCloudPlatform());
        response.setCloudPlatform(source.getCloudPlatform());
        response.setName(source.getName());
        if (source.getAttributes() != null) {
            try {
                CredentialAttributes attributes = new Json(source.getAttributes()).get(CredentialAttributes.class);
                response.setAzure(azureConverter.convert(attributes.getAzure()));
                response.setAws(awsConverter.convert(attributes.getAws()));
                response.setCumulus(cumulusConverter.convert(attributes.getCumulus()));
                response.setGcp(gcpConverter.convert(attributes.getGcp()));
                response.setMock(mockConverter.convert(attributes.getMock()));
                response.setOpenstack(openstackConverter.convert(attributes.getOpenstack()));
                response.setYarn(yarnConverter.convert(attributes.getYarn()));
            } catch (IOException e) {
                throw new BadRequestException("Cannot deserialize the credential's attributes", e);
            }

            //TODO: remove sesitives: Bubba
//            Map<String, Object> parameters = credentialDefinitionService.removeSensitives(platform(source.getCloudPlatform()), secretAttributes.getMap());
//            coverSensitiveData(parameters);
//            credentialParameterSetterUtil.setProperParameters(source.getCloudPlatform(), response, parameters);
            if (response.getAws() != null) {
                response.getAws().setGovCloud(source.getGovCloud());
            }
            response.setAttributes(secretConverter.convert(source.getAttributesSecret()));
        }
        response.setDescription(source.getDescription() == null ? "" : source.getDescription());
        return response;
    }

    public Credential convert(CredentialV1Request source) {
        Credential credential = new Credential();
        credential.setName(source.getName());
        credential.setDescription(source.getDescription());
        credential.setCloudPlatform(source.getCloudPlatform());
        try {
            CredentialAttributes credentialAttributes = new CredentialAttributes();
            credentialAttributes.setAws(awsConverter.convert(source.getAws()));
            credentialAttributes.setAzure(azureConverter.convert(source.getAzure()));
            credentialAttributes.setGcp(gcpConverter.convert(source.getGcp()));
            credentialAttributes.setCumulus(cumulusConverter.convert(source.getCumulus()));
            credentialAttributes.setMock(mockConverter.convert(source.getMock()));
            credentialAttributes.setOpenstack(openstackConverter.convert(source.getOpenstack()));
            credentialAttributes.setYarn(yarnConverter.convert(source.getYarn()));
            credential.setAttributes(new Json(credentialAttributes).getValue());
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Cannot parse parameters", e);
        }
        if (source.getAws() != null) {
            credential.setGovCloud(source.getAws().getGovCloud());
        }
        return credential;
    }

    private void coverSensitiveData(Map<String, Object> params) {
        for (String field : FIELDS_TO_COVER) {
            if (params.get(field) != null) {
                params.put(field, PLACEHOLDER);
            }
        }
    }
}
