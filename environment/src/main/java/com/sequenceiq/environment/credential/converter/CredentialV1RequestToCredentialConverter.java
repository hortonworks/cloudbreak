package com.sequenceiq.environment.credential.converter;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.credential.model.request.CredentialV1Request;
import com.sequenceiq.environment.credential.Credential;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;

@Component
public class CredentialV1RequestToCredentialConverter extends AbstractConversionServiceAwareConverter<CredentialV1Request, Credential> {

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

    @Override
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

}
