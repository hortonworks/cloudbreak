package com.sequenceiq.environment.credential.v1.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import java.io.IOException;
import java.util.UUID;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.environment.api.v1.credential.model.request.EditCredentialRequest;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.domain.CredentialSettings;
import com.sequenceiq.environment.credential.v1.converter.aws.AwsCredentialV1ParametersToAwsCredentialAttributesConverter;
import com.sequenceiq.environment.credential.v1.converter.azure.AzureCredentialRequestParametersToAzureCredentialAttributesConverter;
import com.sequenceiq.environment.credential.v1.converter.gcp.GcpCredentialV1ParametersToGcpCredentialAttributesConverter;
import com.sequenceiq.environment.credential.v1.converter.mock.MockCredentialV1ParametersToMockCredentialAttributesConverter;
import com.sequenceiq.environment.credential.v1.converter.yarn.YarnCredentialV1ParametersToAwsYarnAttributesConverter;

@Component
public class EditCredentialRequestToCredentialConverter {

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

    public Credential convert(EditCredentialRequest editCredentialRequest, Credential originalCredential) {
        if (editCredentialRequest == null) {
            return null;
        }
        Credential credential = new Credential();
        credential.setName(Strings.isNullOrEmpty(editCredentialRequest.getName()) ? UUID.randomUUID().toString() : editCredentialRequest.getName());
        credential.setDescription(editCredentialRequest.getDescription());
        credential.setCloudPlatform(editCredentialRequest.getCloudPlatform());
        credential.setVerificationStatusText(editCredentialRequest.getVerificationStatusText());
        credential.setCredentialSettings(new CredentialSettings(editCredentialRequest.isVerifyPermissions(), editCredentialRequest.isSkipOrgPolicyDecisions()));
        convertAttributes(editCredentialRequest, credential, originalCredential);
        if (editCredentialRequest.getAws() != null) {
            credential.setGovCloud(editCredentialRequest.getAws().getGovCloud());
        }
        return credential;
    }

    private void convertAttributes(EditCredentialRequest source, Credential targetCredential, Credential originalCredential) {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        doIfNotNull(source.getAws(), params -> credentialAttributes.setAws(awsConverter.convert(params)));
        try {
            if (source.getAzure() != null) {
                CredentialAttributes originalCredentialAttributes = null;
                if (originalCredential != null) {
                    originalCredentialAttributes = JsonUtil.readValue(originalCredential.getAttributes(), CredentialAttributes.class);
                }
                credentialAttributes.setAzure(azureConverter.convertModify(source.getAzure(), originalCredentialAttributes));
            }
        } catch (IOException e) {
            throw new RuntimeException("We were unable to parse the stored credential, therefore editing is not possible", e);
        }
        doIfNotNull(source.getGcp(), params -> credentialAttributes.setGcp(gcpConverter.convert(params)));
        doIfNotNull(source.getMock(), params -> credentialAttributes.setMock(mockConverter.convert(params)));
        doIfNotNull(source.getYarn(), params -> credentialAttributes.setYarn(yarnConverter.convert(params)));
        targetCredential.setAttributes(new Json(credentialAttributes).getValue());
    }
}
