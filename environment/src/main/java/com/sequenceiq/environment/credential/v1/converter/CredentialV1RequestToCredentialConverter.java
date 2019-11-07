package com.sequenceiq.environment.credential.v1.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.domain.Credential;

@Component
public class CredentialV1RequestToCredentialConverter {

    @Inject
    private AwsCredentialV1ParametersToAwsCredentialAttributesConverter awsConverter;

    @Inject
    private AzureCredentialV1ParametersToAzureCredentialAttributesConverter azureConverter;

    @Inject
    private GcpCredentialV1ParametersToGcpCredentialAttributesConverter gcpConverter;

    @Inject
    private MockCredentialV1ParametersToMockCredentialAttributesConverter mockConverter;

    @Inject
    private OpenStackCredentialV1ParametersToOpenStackCredentialAttributesConverter openstackConverter;

    @Inject
    private YarnCredentialV1ParametersToAwsYarnAttributesConverter yarnConverter;

    public Credential convert(CredentialRequest source) {
        Credential credential = new Credential();
        credential.setName(source.getName());
        credential.setDescription(source.getDescription());
        credential.setCloudPlatform(source.getCloudPlatform());
        credential.setVerificationStatusText(source.getVerificationStatusText());
        convertAttributes(source, credential);
        if (source.getAws() != null) {
            credential.setGovCloud(source.getAws().getGovCloud());
        }
        return credential;
    }

    private void convertAttributes(CredentialRequest source, Credential credential) {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        doIfNotNull(source.getAws(), param -> credentialAttributes.setAws(awsConverter.convert(param)));
        doIfNotNull(source.getAzure(), param -> credentialAttributes.setAzure(azureConverter.convert(param)));
        doIfNotNull(source.getGcp(), param -> credentialAttributes.setGcp(gcpConverter.convert(param)));
        doIfNotNull(source.getMock(), param -> credentialAttributes.setMock(mockConverter.convert(param)));
        doIfNotNull(source.getOpenstack(), param -> credentialAttributes.setOpenstack(openstackConverter.convert(param)));
        doIfNotNull(source.getYarn(), param -> credentialAttributes.setYarn(yarnConverter.convert(param)));
        credential.setAttributes(new Json(credentialAttributes).getValue());
    }

}
