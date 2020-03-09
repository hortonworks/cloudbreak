package com.sequenceiq.environment.environment.v1.cli.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.cloudera.cdp.environments.model.CreateAzureEnvironmentRequest;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAuthenticationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.LocationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.SecurityAccessRequest;
import com.sequenceiq.environment.network.v1.AzureRegistrationTypeResolver;

class EnvironmentRequestToCreateAzureEnvironmentRequestConverterTest {
    private final AzureRegistrationTypeResolver azureRegistrationTypeResolver = new AzureRegistrationTypeResolver();

    private final EnvironmentRequestToCreateAzureEnvironmentRequestConverter underTest
            = new EnvironmentRequestToCreateAzureEnvironmentRequestConverter(azureRegistrationTypeResolver);

    @Test
    void convertWithExistingNetwork() {
        EnvironmentRequest environmentRequest = getEnvironmentRequest();
        CreateAzureEnvironmentRequest result = underTest.convert(environmentRequest);
        assertEquals(environmentRequest.getCredentialName(), result.getCredentialName());
        assertEquals(environmentRequest.getDescription(), result.getDescription());
        assertEquals(environmentRequest.getName(), result.getEnvironmentName());
        assertEquals(environmentRequest.getTelemetry().getLogging().getStorageLocation(), result.getLogStorage().getStorageLocationBase());
        assertEquals(environmentRequest.getTelemetry().getLogging().getAdlsGen2().getManagedIdentity(), result.getLogStorage().getManagedIdentity());
        assertEquals(environmentRequest.getLocation().getName(), result.getRegion());
        assertEquals(environmentRequest.getSecurityAccess().getCidr(), result.getSecurityAccess().getCidr());
        assertEquals(environmentRequest.getSecurityAccess().getDefaultSecurityGroupId(), result.getSecurityAccess().getDefaultSecurityGroupId());
        assertEquals(environmentRequest.getSecurityAccess().getSecurityGroupIdForKnox(), result.getSecurityAccess().getSecurityGroupIdForKnox());
        assertEquals(environmentRequest.getNetwork().getAzure().getResourceGroupName(), result.getExistingNetworkParams().getResourceGroupName());
        assertThat(result.getExistingNetworkParams().getSubnetIds()).hasSameElementsAs(environmentRequest.getNetwork().getSubnetIds());
    }

    @Test
    void convertWithNewNetwork() {
        EnvironmentRequest environmentRequest = getEnvironmentRequest();
        environmentRequest.getNetwork().getAzure().setNetworkId(null);
        CreateAzureEnvironmentRequest result = underTest.convert(environmentRequest);
        assertEquals(environmentRequest.getCredentialName(), result.getCredentialName());
        assertEquals(environmentRequest.getDescription(), result.getDescription());
        assertEquals(environmentRequest.getName(), result.getEnvironmentName());
        assertEquals(environmentRequest.getTelemetry().getLogging().getStorageLocation(), result.getLogStorage().getStorageLocationBase());
        assertEquals(environmentRequest.getTelemetry().getLogging().getAdlsGen2().getManagedIdentity(), result.getLogStorage().getManagedIdentity());
        assertEquals(environmentRequest.getLocation().getName(), result.getRegion());
        assertEquals(environmentRequest.getSecurityAccess().getCidr(), result.getSecurityAccess().getCidr());
        assertEquals(environmentRequest.getSecurityAccess().getDefaultSecurityGroupId(), result.getSecurityAccess().getDefaultSecurityGroupId());
        assertEquals(environmentRequest.getSecurityAccess().getSecurityGroupIdForKnox(), result.getSecurityAccess().getSecurityGroupIdForKnox());
        assertEquals(environmentRequest.getNetwork().getNetworkCidr(), result.getNewNetworkParams().getNetworkCidr());
    }

    private EnvironmentRequest getEnvironmentRequest() {
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        EnvironmentAuthenticationRequest authentication = new EnvironmentAuthenticationRequest();
        authentication.setPublicKey("key");
        authentication.setPublicKeyId("keyId");
        environmentRequest.setAuthentication(authentication);
        environmentRequest.setCredentialName("credentialName");
        environmentRequest.setDescription("desc");
        environmentRequest.setName("envName");
        TelemetryRequest telemetry = new TelemetryRequest();
        LoggingRequest logging = new LoggingRequest();
        logging.setStorageLocation("storageLocation");
        AdlsGen2CloudStorageV1Parameters adls = new AdlsGen2CloudStorageV1Parameters();
        adls.setManagedIdentity("ManagedIdentity");
        logging.setAdlsGen2(adls);
        telemetry.setLogging(logging);
        environmentRequest.setTelemetry(telemetry);
        EnvironmentNetworkAzureParams azureParams = new EnvironmentNetworkAzureParams();
        azureParams.setNoPublicIp(true);
        azureParams.setResourceGroupName("ResourceGroupName");
        azureParams.setNetworkId("NetworkId");
        Set<String> subnets = Set.of("subnet1", "subnet2");
        EnvironmentNetworkRequest network = new EnvironmentNetworkRequest();
        network.setNetworkCidr("networkCidr");
        network.setSubnetIds(subnets);
        network.setAzure(azureParams);
        environmentRequest.setNetwork(network);
        LocationRequest location = new LocationRequest();
        location.setName("region");
        environmentRequest.setLocation(location);
        SecurityAccessRequest securityAccess = new SecurityAccessRequest();
        securityAccess.setCidr("securityCidr");
        securityAccess.setSecurityGroupIdForKnox("knoxSG");
        securityAccess.setDefaultSecurityGroupId("defaultSG");
        environmentRequest.setSecurityAccess(securityAccess);
        return environmentRequest;
    }
}