package com.sequenceiq.environment.network.service;

import static com.sequenceiq.environment.network.service.Cidrs.cidrs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagMergeRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
class NetworkCreationRequestFactoryTest {

    private static final String REGION = "us-west-1";

    private static final String ENV_NAME = "testEnv";

    private static final String STACK_NAME = "testEnv-1";

    private static final String NETWORK_CIDR = "1.1.1.1/16";

    private static final long NETWORK_ID = 1L;

    private static final String CLOUD_PLATFORM = "AWS";

    private static final Set<String> SUBNET_CIDRS = Collections.singleton("10.10.1.1/24");

    private final DefaultSubnetCidrProvider defaultSubnetCidrProvider = Mockito.mock(DefaultSubnetCidrProvider.class);

    private final CostTagging costTagging = Mockito.mock(CostTagging.class);

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter = Mockito.mock(CredentialToCloudCredentialConverter.class);

    private final NetworkCreationRequestFactory underTest = new NetworkCreationRequestFactory(defaultSubnetCidrProvider,
            credentialToCloudCredentialConverter, costTagging);

    @Test
    void testCreateShouldCreateANetworkCreationRequestWhenAzureParamsAreNotPresent() {
        EnvironmentDto environmentDto = createEnvironmentDtoWithoutAureParams().build();
        CloudCredential cloudCredential = new CloudCredential("1", "asd");

        when(credentialToCloudCredentialConverter.convert(environmentDto.getCredential())).thenReturn(cloudCredential);
        when(defaultSubnetCidrProvider.provide(NETWORK_CIDR)).thenReturn(cidrs(SUBNET_CIDRS, new HashSet<>()));
        when(costTagging.mergeTags(any(CDPTagMergeRequest.class))).thenReturn(new HashMap<>());
        when(costTagging.mergeTags(any(CDPTagMergeRequest.class))).thenReturn(new HashMap<>());

        NetworkCreationRequest actual = underTest.create(environmentDto);

        verify(credentialToCloudCredentialConverter).convert(environmentDto.getCredential());
        verify(defaultSubnetCidrProvider).provide(NETWORK_CIDR);
        assertEquals(ENV_NAME, actual.getEnvName());
        assertEquals(STACK_NAME, actual.getStackName());
        assertEquals(cloudCredential, actual.getCloudCredential());
        assertEquals(CLOUD_PLATFORM, actual.getVariant());
        assertEquals(REGION, actual.getRegion().value());
        assertEquals(NETWORK_CIDR, actual.getNetworkCidr());
        assertEquals(SUBNET_CIDRS, actual.getPublicSubnetCidrs());
        assertFalse(actual.isNoPublicIp());
    }

    @Test
    void testCreateShouldCreateANetworkCreationRequestWhenAzureParamsArePresent() {
        EnvironmentDto environmentDto = createEnvironmentDtoWithAureParams().build();
        CloudCredential cloudCredential = new CloudCredential("1", "asd");

        when(credentialToCloudCredentialConverter.convert(environmentDto.getCredential())).thenReturn(cloudCredential);
        when(defaultSubnetCidrProvider.provide(NETWORK_CIDR)).thenReturn(cidrs(SUBNET_CIDRS, new HashSet<>()));

        NetworkCreationRequest actual = underTest.create(environmentDto);

        verify(credentialToCloudCredentialConverter).convert(environmentDto.getCredential());
        verify(defaultSubnetCidrProvider).provide(NETWORK_CIDR);

        assertEquals(ENV_NAME, actual.getEnvName());
        assertEquals(STACK_NAME, actual.getStackName());
        assertEquals(cloudCredential, actual.getCloudCredential());
        assertEquals(CLOUD_PLATFORM, actual.getVariant());
        assertEquals(REGION, actual.getRegion().value());
        assertEquals(NETWORK_CIDR, actual.getNetworkCidr());
        assertEquals(SUBNET_CIDRS, actual.getPublicSubnetCidrs());
        assertTrue(actual.isNoPublicIp());
    }

    private EnvironmentDto.Builder createEnvironmentDtoWithoutAureParams() {
        return EnvironmentDto.builder()
                .withName(ENV_NAME)
                .withTags(new EnvironmentTags(new HashMap<>(), new HashMap<>()))
                .withCreator("creator")
                .withCredential(new Credential())
                .withCloudPlatform(CLOUD_PLATFORM)
                .withLocationDto(LocationDto.builder().withName(REGION).build())
                .withNetwork(NetworkDto.builder().withId(NETWORK_ID).withNetworkCidr(NETWORK_CIDR).build());
    }

    private EnvironmentDto.Builder createEnvironmentDtoWithAureParams() {
        EnvironmentDto.Builder builder = createEnvironmentDtoWithoutAureParams();
        builder.withNetwork(NetworkDto.builder()
                .withId(NETWORK_ID)
                .withNetworkCidr(NETWORK_CIDR)
                .withAzure(AzureParams.AzureParamsBuilder.anAzureParams()
                        .withNoPublicIp(true)
                        .build())
                .build());
        return builder;
    }

    public SubnetRequest publicSubnetRequest(String cidr, int index) {
        SubnetRequest subnetRequest = new SubnetRequest();
        subnetRequest.setIndex(index);
        subnetRequest.setPublicSubnetCidr(cidr);
        subnetRequest.setSubnetGroup(index % 3);
        subnetRequest.setAvailabilityZone("az");
        return subnetRequest;
    }

}
