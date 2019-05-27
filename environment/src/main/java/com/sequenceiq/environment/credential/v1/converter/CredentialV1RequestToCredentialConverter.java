package com.sequenceiq.environment.credential.v1.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.ifNotNull;

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
    private CumulusCredentialV1ParametersToCumulusCredentialAttributesConverter cumulusConverter;

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
        convertAttributes(source, credential);
        if (source.getAws() != null) {
            credential.setGovCloud(source.getAws().getGovCloud());
        }
        return credential;
    }

    private void convertAttributes(CredentialRequest source, Credential credential) {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        ifNotNull(source.getAws(), param -> credentialAttributes.setAws(awsConverter.convert(param)));
        ifNotNull(source.getAzure(), param -> credentialAttributes.setAzure(azureConverter.convert(param)));
        ifNotNull(source.getGcp(), param -> credentialAttributes.setGcp(gcpConverter.convert(param)));
        ifNotNull(source.getCumulus(), param -> credentialAttributes.setCumulus(cumulusConverter.convert(param)));
        ifNotNull(source.getMock(), param -> credentialAttributes.setMock(mockConverter.convert(param)));
        ifNotNull(source.getOpenstack(), param -> credentialAttributes.setOpenstack(openstackConverter.convert(param)));
        ifNotNull(source.getYarn(), param -> credentialAttributes.setYarn(yarnConverter.convert(param)));
        credential.setAttributes(new Json(credentialAttributes).getValue());
    }

}
