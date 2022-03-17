package com.sequenceiq.environment.credential.v1.converter;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.request.EditCredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.aws.AwsCredentialV1ParametersToAwsCredentialAttributesConverter;
import com.sequenceiq.environment.credential.v1.converter.azure.AzureCredentialV1ParametersToAzureCredentialAttributesConverter;
import com.sequenceiq.environment.credential.v1.converter.gcp.GcpCredentialV1ParametersToGcpCredentialAttributesConverter;
import com.sequenceiq.environment.credential.v1.converter.mock.MockCredentialV1ParametersToMockCredentialAttributesConverter;
import com.sequenceiq.environment.credential.v1.converter.yarn.YarnCredentialV1ParametersToAwsYarnAttributesConverter;
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
    private GcpCredentialV1ParametersToGcpCredentialAttributesConverter gcpConverter;

    @Inject
    private MockCredentialV1ParametersToMockCredentialAttributesConverter mockConverter;

    @Inject
    private YarnCredentialV1ParametersToAwsYarnAttributesConverter yarnConverter;

    @Inject
    private StringToSecretResponseConverter secretConverter;

    public CredentialResponse convert(Credential source) {
        CredentialResponse response = new CredentialResponse();
        credentialValidator.validateCredentialCloudPlatform(source.getCloudPlatform(), source.getCreator(), source.getType());
        response.setCloudPlatform(source.getCloudPlatform());
        response.setName(source.getName());
        response.setVerifyPermissions(source.isVerifyPermissions());
        response.setVerificationStatusText(source.getVerificationStatusText());
        if (source.getAttributes() != null) {
            convertAttributes(source, response);
            if (response.getAws() != null) {
                response.getAws().setGovCloud(source.getGovCloud());
            }
            response.setAttributes(secretConverter.convert(source.getAttributesSecret()));
        }
        response.setCrn(source.getResourceCrn());
        response.setCreator(source.getCreator());
        response.setCreated(source.getCreated());
        response.setDescription(source.getDescription() == null ? "" : source.getDescription());
        response.setType(source.getType());
        response.setGovCloud(source.getGovCloud());
        response.setAccountId(source.getAccountId());
        return response;
    }

    private void convertAttributes(Credential source, CredentialResponse response) {
        try {
            Json json = new Json(source.getAttributes());
            credentialDefinitionService.checkPropertiesRemoveSensitives(platform(source.getCloudPlatform()), json);
            coverSensitiveData(json);
            CredentialAttributes credentialAttributes = json.get(CredentialAttributes.class);
            doIfNotNull(credentialAttributes.getAws(), param -> response.setAws(awsConverter.convert(param)));
            doIfNotNull(credentialAttributes.getAzure(), param -> response.setAzure(azureConverter.convert(param)));
            doIfNotNull(credentialAttributes.getGcp(), param -> response.setGcp(gcpConverter.convert(param)));
            doIfNotNull(credentialAttributes.getMock(), param -> response.setMock(mockConverter.convert(param)));
            doIfNotNull(credentialAttributes.getYarn(), param -> response.setYarn(yarnConverter.convert(param)));
        } catch (IOException e) {
            throw new BadRequestException("Cannot deserialize the credential's attributes", e);
        }
    }

    public Credential convert(CredentialRequest source) {
        if (source == null) {
            return null;
        }
        Credential credential = new Credential();
        credential.setName(Strings.isNullOrEmpty(source.getName()) ? UUID.randomUUID().toString() : source.getName());
        credential.setDescription(source.getDescription());
        credential.setCloudPlatform(source.getCloudPlatform());
        credential.setVerificationStatusText(source.getVerificationStatusText());
        credential.setVerifyPermissions(source.isVerifyPermissions());
        convertAttributes(source, credential);
        if (source.getAws() != null) {
            credential.setGovCloud(source.getAws().getGovCloud());
        }
        return credential;
    }

    public Credential convert(EditCredentialRequest source) {
        if (source == null) {
            return null;
        }
        Credential credential = new Credential();
        credential.setName(Strings.isNullOrEmpty(source.getName()) ? UUID.randomUUID().toString() : source.getName());
        credential.setDescription(source.getDescription());
        credential.setCloudPlatform(source.getCloudPlatform());
        credential.setVerificationStatusText(source.getVerificationStatusText());
        credential.setVerifyPermissions(source.isVerifyPermissions());
        convertAttributes(source, credential);
        if (source.getAws() != null) {
            credential.setGovCloud(source.getAws().getGovCloud());
        }
        return credential;
    }

    public Credential convert(ExtendedCloudCredential source) {
        if (source == null) {
            return null;
        }
        Credential credential = new Credential();
        credential.setName(source.getName());
        credential.setDescription(source.getDescription());
        credential.setCloudPlatform(source.getCloudPlatform());
        credential.setCreator(source.getUserCrn());
        credential.setAccountId(source.getAccountId());

        Map<String, Object> attributes = source.getParameters() == null ? new HashMap<>() : source.getParameters();
        credential.setAttributes(new Json(attributes).getValue());
        return credential;
    }

    private void convertAttributes(CredentialRequest source, Credential credential) {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        doIfNotNull(source.getAws(), params -> credentialAttributes.setAws(awsConverter.convert(params)));
        doIfNotNull(source.getAzure(), params -> credentialAttributes.setAzure(azureConverter.convert(params)));
        doIfNotNull(source.getGcp(), params -> credentialAttributes.setGcp(gcpConverter.convert(params)));
        doIfNotNull(source.getMock(), params -> credentialAttributes.setMock(mockConverter.convert(params)));
        doIfNotNull(source.getYarn(), params -> credentialAttributes.setYarn(yarnConverter.convert(params)));
        credential.setAttributes(new Json(credentialAttributes).getValue());
    }

    private void convertAttributes(EditCredentialRequest source, Credential credential) {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        doIfNotNull(source.getAws(), params -> credentialAttributes.setAws(awsConverter.convert(params)));
        doIfNotNull(source.getAzure(), params -> credentialAttributes.setAzure(azureConverter.convert(params)));
        doIfNotNull(source.getGcp(), params -> credentialAttributes.setGcp(gcpConverter.convert(params)));
        doIfNotNull(source.getMock(), params -> credentialAttributes.setMock(mockConverter.convert(params)));
        doIfNotNull(source.getYarn(), params -> credentialAttributes.setYarn(yarnConverter.convert(params)));
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
