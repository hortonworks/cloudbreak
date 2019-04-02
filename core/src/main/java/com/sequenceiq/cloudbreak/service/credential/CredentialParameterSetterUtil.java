package com.sequenceiq.cloudbreak.service.credential;

import static com.sequenceiq.cloudbreak.converter.v4.credentials.ParameterMapToClassConverterUtil.exec;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.AwsCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AzureCredentialV4ResponseParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.cumulus.CumulusYarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.GcpCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.OpenstackCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.yarn.YarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class CredentialParameterSetterUtil {

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    public void setProperParameters(String cloudPlatform, CredentialV4Response response, Map<String, Object> parameters) {
        if (CloudPlatform.AWS.name().equals(cloudPlatform)) {
            response.setAws(conversionService.convert(parameters, AwsCredentialV4Parameters.class));
        } else if (CloudPlatform.GCP.name().equals(cloudPlatform)) {
            response.setGcp(conversionService.convert(parameters, GcpCredentialV4Parameters.class));
        } else if (CloudPlatform.YARN.name().equals(cloudPlatform)) {
            response.setYarn(exec(() -> new Json(parameters).get(YarnCredentialV4Parameters.class), YarnCredentialV4Parameters.class));
        } else if (CloudPlatform.CUMULUS_YARN.name().equals(cloudPlatform)) {
            response.setCumulus(exec(() -> new Json(parameters).get(CumulusYarnCredentialV4Parameters.class), CumulusYarnCredentialV4Parameters.class));
        } else if (CloudPlatform.AZURE.name().equals(cloudPlatform)) {
            response.setAzure(conversionService.convert(parameters, AzureCredentialV4ResponseParameters.class));
        } else if (CloudPlatform.OPENSTACK.name().equals(cloudPlatform)) {
            response.setOpenstack(conversionService.convert(parameters, OpenstackCredentialV4Parameters.class));
        }
    }

}
