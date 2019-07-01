package com.sequenceiq.environment.network.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@RunWith(MockitoJUnitRunner.class)
public class NetworkCreationRequestFactoryTest {

    private static final String REGION = "us-west-1";

    private static final String ENV_NAME = "testEnv";

    private static final String STACK_NAME = "testEnv-1";

    private static final String NETWORK_CIDR = "1.1.1.1/16";

    private static final long NETWORK_ID = 1L;

    private static final String CLOUD_PLATFORM = "AWS";

    private static final Set<String> SUBNET_CIDRS = Collections.singleton("10.10.1.1/24");

    private final SubnetCidrProvider subnetCidrProvider = Mockito.mock(SubnetCidrProvider.class);

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter = Mockito.mock(CredentialToCloudCredentialConverter.class);

    private final NetworkCreationRequestFactory underTest = new NetworkCreationRequestFactory(subnetCidrProvider, credentialToCloudCredentialConverter);

    @Test
    public void testCreateShouldCreateANetworkCreationRequestWhenAzureParamsAreNotPresent() {
        EnvironmentDto environmentDto = createEnvironmentDtoWithoutAureParams().build();
        CloudCredential cloudCredential = new CloudCredential("1", "asd");

        when(credentialToCloudCredentialConverter.convert(environmentDto.getCredential())).thenReturn(cloudCredential);
        when(subnetCidrProvider.provide(NETWORK_CIDR)).thenReturn(SUBNET_CIDRS);

        NetworkCreationRequest actual = underTest.create(environmentDto);

        verify(credentialToCloudCredentialConverter).convert(environmentDto.getCredential());
        verify(subnetCidrProvider).provide(NETWORK_CIDR);
        assertEquals(ENV_NAME, actual.getEnvName());
        assertEquals(STACK_NAME, actual.getStackName());
        assertEquals(cloudCredential, actual.getCloudCredential());
        assertEquals(CLOUD_PLATFORM, actual.getVariant());
        assertEquals(REGION, actual.getRegion().value());
        assertEquals(NETWORK_CIDR, actual.getNetworkCidr());
        assertEquals(SUBNET_CIDRS, actual.getSubnetCidrs());
        assertFalse(actual.isNoFirewallRules());
        assertFalse(actual.isNoPublicIp());
    }

    @Test
    public void testCreateShouldCreateANetworkCreationRequestWhenAzureParamsArePresent() {
        EnvironmentDto environmentDto = createEnvironmentDtoWithAureParams().build();
        CloudCredential cloudCredential = new CloudCredential("1", "asd");

        when(credentialToCloudCredentialConverter.convert(environmentDto.getCredential())).thenReturn(cloudCredential);
        when(subnetCidrProvider.provide(NETWORK_CIDR)).thenReturn(SUBNET_CIDRS);

        NetworkCreationRequest actual = underTest.create(environmentDto);

        verify(credentialToCloudCredentialConverter).convert(environmentDto.getCredential());
        verify(subnetCidrProvider).provide(NETWORK_CIDR);

        assertEquals(ENV_NAME, actual.getEnvName());
        assertEquals(STACK_NAME, actual.getStackName());
        assertEquals(cloudCredential, actual.getCloudCredential());
        assertEquals(CLOUD_PLATFORM, actual.getVariant());
        assertEquals(REGION, actual.getRegion().value());
        assertEquals(NETWORK_CIDR, actual.getNetworkCidr());
        assertEquals(SUBNET_CIDRS, actual.getSubnetCidrs());
        assertTrue(actual.isNoFirewallRules());
        assertTrue(actual.isNoPublicIp());
    }

    private EnvironmentDto.Builder createEnvironmentDtoWithoutAureParams() {
        return EnvironmentDto.builder()
                .withName(ENV_NAME)
                .withCredential(new Credential())
                .withCloudPlatform(CLOUD_PLATFORM)
                .withLocationDto(LocationDto.LocationDtoBuilder.aLocationDto().withName(REGION).build())
                .withNetwork(NetworkDto.Builder.aNetworkDto().withId(NETWORK_ID).withNetworkCidr(NETWORK_CIDR).build());
    }

    private EnvironmentDto.Builder createEnvironmentDtoWithAureParams() {
        EnvironmentDto.Builder builder = createEnvironmentDtoWithoutAureParams();
        builder.withNetwork(NetworkDto.Builder.aNetworkDto()
                .withId(NETWORK_ID)
                .withNetworkCidr(NETWORK_CIDR)
                .withAzure(AzureParams.AzureParamsBuilder.anAzureParams()
                        .withNoFirewallRules(true)
                        .withNoPublicIp(true)
                        .build())
                .build());
        return builder;
    }

}