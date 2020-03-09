package com.sequenceiq.environment.environment.v1.cli.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.cloudera.cdp.environments.model.CreateAzureEnvironmentRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.v1.AzureRegistrationTypeResolver;
import com.sequenceiq.environment.parameters.dto.ParametersDto;

class EnvironmentDtoToCreateAzureEnvironmentRequestConverterTest {

    private final AzureRegistrationTypeResolver azureRegistrationTypeResolver = new AzureRegistrationTypeResolver();

    private final EnvironmentDtoToCreateAzureEnvironmentRequestConverter underTest
            = new EnvironmentDtoToCreateAzureEnvironmentRequestConverter(azureRegistrationTypeResolver);

    @Test
    void convertWithExistingNetwork() {
        EnvironmentDto environmentDto = getEnvironmentDto();
        CreateAzureEnvironmentRequest result = underTest.convert(environmentDto);
        assertEquals(environmentDto.getCredential().getName(), result.getCredentialName());
        assertEquals(environmentDto.getDescription(), result.getDescription());
        assertEquals(environmentDto.getName(), result.getEnvironmentName());
        assertEquals(environmentDto.getTelemetry().getLogging().getStorageLocation(), result.getLogStorage().getStorageLocationBase());
        assertEquals(environmentDto.getTelemetry().getLogging().getAdlsGen2().getManagedIdentity(), result.getLogStorage().getManagedIdentity());
        assertEquals(environmentDto.getLocation().getName(), result.getRegion());
        assertEquals(environmentDto.getSecurityAccess().getCidr(), result.getSecurityAccess().getCidr());
        assertEquals(environmentDto.getSecurityAccess().getDefaultSecurityGroupId(), result.getSecurityAccess().getDefaultSecurityGroupId());
        assertEquals(environmentDto.getSecurityAccess().getSecurityGroupIdForKnox(), result.getSecurityAccess().getSecurityGroupIdForKnox());
        assertEquals(environmentDto.getNetwork().getAzure().getResourceGroupName(), result.getExistingNetworkParams().getResourceGroupName());
        assertThat(result.getExistingNetworkParams().getSubnetIds()).hasSameElementsAs(environmentDto.getNetwork().getSubnetIds());
    }

    @Test
    void convertWithNewNetwork() {
        EnvironmentDto environmentDto = getEnvironmentDto();
        environmentDto.getNetwork().getAzure().setNetworkId(null);
        CreateAzureEnvironmentRequest result = underTest.convert(environmentDto);
        assertEquals(environmentDto.getCredential().getName(), result.getCredentialName());
        assertEquals(environmentDto.getDescription(), result.getDescription());
        assertEquals(environmentDto.getName(), result.getEnvironmentName());
        assertEquals(environmentDto.getTelemetry().getLogging().getStorageLocation(), result.getLogStorage().getStorageLocationBase());
        assertEquals(environmentDto.getTelemetry().getLogging().getAdlsGen2().getManagedIdentity(), result.getLogStorage().getManagedIdentity());
        assertEquals(environmentDto.getLocation().getName(), result.getRegion());
        assertEquals(environmentDto.getSecurityAccess().getCidr(), result.getSecurityAccess().getCidr());
        assertEquals(environmentDto.getSecurityAccess().getDefaultSecurityGroupId(), result.getSecurityAccess().getDefaultSecurityGroupId());
        assertEquals(environmentDto.getSecurityAccess().getSecurityGroupIdForKnox(), result.getSecurityAccess().getSecurityGroupIdForKnox());
        assertEquals(environmentDto.getNetwork().getNetworkCidr(), result.getNewNetworkParams().getNetworkCidr());
    }

    private EnvironmentDto getEnvironmentDto() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        AuthenticationDto authentication = AuthenticationDto.builder()
                .withPublicKey("key")
                .withPublicKeyId("keyid")
                .build();
        environmentDto.setAuthentication(authentication);
        Credential credential = new Credential();
        credential.setName("credentialName");
        environmentDto.setCredential(credential);
        environmentDto.setDescription("desc");
        environmentDto.setName("envName");
        EnvironmentTelemetry telemetry = new EnvironmentTelemetry();
        EnvironmentLogging logging = new EnvironmentLogging();
        logging.setStorageLocation("storageLocation");
        AdlsGen2CloudStorageV1Parameters adls = new AdlsGen2CloudStorageV1Parameters();
        adls.setManagedIdentity("ManagedIdentity");
        logging.setAdlsGen2(adls);
        telemetry.setLogging(logging);
        environmentDto.setTelemetry(telemetry);
        AzureParams azureParams = new AzureParams();
        azureParams.setNoPublicIp(true);
        azureParams.setResourceGroupName("ResourceGroupName");
        azureParams.setNetworkId("NetworkId");
        Map<String, CloudSubnet> subnets = Map.of("subnet1", new CloudSubnet(), "subnet2", new CloudSubnet());
        NetworkDto network = NetworkDto.builder()
                .withNetworkCidr("networkCidr")
                .withAzure(azureParams)
                .withSubnetMetas(subnets)
                .build();
        environmentDto.setNetwork(network);
        LocationDto location = LocationDto.builder()
                .withName("region")
                .build();
        environmentDto.setLocation(location);
        ParametersDto params = ParametersDto.builder()
                .build();
        environmentDto.setParameters(params);
        SecurityAccessDto securityAccess = SecurityAccessDto.builder()
                .withCidr("securityCidr")
                .withDefaultSecurityGroupId("defaultSG")
                .withSecurityGroupIdForKnox("knoxSG")
                .build();
        environmentDto.setSecurityAccess(securityAccess);
        return environmentDto;
    }

}