package com.sequenceiq.cloudbreak.converter.v4.credentials;

import static com.sequenceiq.cloudbreak.converter.v4.credentials.ParameterMapToClassConverterUtil.exec;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.AwsCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AzureCredentialV4RequestParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.cumulus.CumulusYarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.GcpCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.OpenstackCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.yarn.YarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
import com.sequenceiq.cloudbreak.controller.validation.credential.CredentialValidator;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class CredentialToCredentialV4RequestConverter
        extends AbstractConversionServiceAwareConverter<Credential, CredentialV4Request> {

    @Inject
    private CredentialValidator credentialValidator;

    @Override
    public CredentialV4Request convert(Credential source) {
        CredentialV4Request credentialRequest = new CredentialV4Request();
        credentialRequest.setName("");
        credentialRequest.setDescription(source.getDescription());
        credentialValidator.validateCredentialCloudPlatform(source.cloudPlatform());
        credentialRequest.setCloudPlatform(source.cloudPlatform());
        Map<String, Object> parameters = cleanMap(new Json(source.getAttributes()).getMap());
        if (CloudPlatform.AWS.name().equals(source.cloudPlatform())) {
            credentialRequest.setAws(getConversionService().convert(parameters, AwsCredentialV4Parameters.class));
        } else if (CloudPlatform.GCP.name().equals(source.cloudPlatform())) {
            credentialRequest.setGcp(getConversionService().convert(parameters, GcpCredentialV4Parameters.class));
        } else if (CloudPlatform.YARN.name().equals(source.cloudPlatform())) {
            credentialRequest.setYarn(exec(() -> new Json(parameters).get(YarnCredentialV4Parameters.class), YarnCredentialV4Parameters.class));
        } else if (CloudPlatform.CUMULUS_YARN.name().equals(source.cloudPlatform())) {
            credentialRequest.setCumulus(exec(() -> new Json(parameters).get(CumulusYarnCredentialV4Parameters.class), CumulusYarnCredentialV4Parameters.class));
        } else if (CloudPlatform.AZURE.name().equals(source.cloudPlatform())) {
            credentialRequest.setAzure(exec(() -> new Json(parameters).get(AzureCredentialV4RequestParameters.class), AzureCredentialV4RequestParameters.class));
        } else if (CloudPlatform.OPENSTACK.name().equals(source.cloudPlatform())) {
            credentialRequest.setOpenstack(getConversionService().convert(parameters, OpenstackCredentialV4Parameters.class));
        }
        return credentialRequest;
    }
}
