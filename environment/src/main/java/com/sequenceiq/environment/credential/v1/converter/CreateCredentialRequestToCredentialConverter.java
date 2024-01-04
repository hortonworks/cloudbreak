package com.sequenceiq.environment.credential.v1.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import java.util.UUID;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.domain.CredentialSettings;
import com.sequenceiq.environment.credential.v1.converter.aws.AwsCredentialV1ParametersToAwsCredentialAttributesConverter;
import com.sequenceiq.environment.credential.v1.converter.azure.AzureCredentialRequestParametersToAzureCredentialAttributesConverter;
import com.sequenceiq.environment.credential.v1.converter.gcp.GcpCredentialV1ParametersToGcpCredentialAttributesConverter;
import com.sequenceiq.environment.credential.v1.converter.mock.MockCredentialV1ParametersToMockCredentialAttributesConverter;
import com.sequenceiq.environment.credential.v1.converter.yarn.YarnCredentialV1ParametersToAwsYarnAttributesConverter;

@Component
public class CreateCredentialRequestToCredentialConverter {

    @Inject
    private AwsCredentialV1ParametersToAwsCredentialAttributesConverter awsConverter;

    @Inject
    private AzureCredentialRequestParametersToAzureCredentialAttributesConverter azureConverter;

    @Inject
    private GcpCredentialV1ParametersToGcpCredentialAttributesConverter gcpConverter;

    @Inject
    private MockCredentialV1ParametersToMockCredentialAttributesConverter mockConverter;

    @Inject
    private YarnCredentialV1ParametersToAwsYarnAttributesConverter yarnConverter;

    public Credential convert(CredentialRequest source) {
        if (source == null) {
            return null;
        }
        Credential credential = new Credential();
        credential.setName(Strings.isNullOrEmpty(source.getName()) ? UUID.randomUUID().toString() : source.getName());
        credential.setDescription(source.getDescription());
        credential.setCloudPlatform(source.getCloudPlatform());
        credential.setVerificationStatusText(source.getVerificationStatusText());
        credential.setCredentialSettings(new CredentialSettings(source.isVerifyPermissions(), source.isSkipOrgPolicyDecisions()));
        convertAttributes(source, credential);
        if (source.getAws() != null) {
            credential.setGovCloud(source.getAws().getGovCloud());
        }
        return credential;
    }

    private void convertAttributes(CredentialRequest source, Credential credential) {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        doIfNotNull(source.getAws(), param -> credentialAttributes.setAws(awsConverter.convert(param)));
        doIfNotNull(source.getAzure(), param -> credentialAttributes.setAzure(azureConverter.convertCreate(param)));
        doIfNotNull(source.getGcp(), param -> credentialAttributes.setGcp(gcpConverter.convert(param)));
        doIfNotNull(source.getMock(), param -> credentialAttributes.setMock(mockConverter.convert(param)));
        doIfNotNull(source.getYarn(), param -> credentialAttributes.setYarn(yarnConverter.convert(param)));
        credential.setAttributes(new Json(credentialAttributes).getValue());
    }

}
