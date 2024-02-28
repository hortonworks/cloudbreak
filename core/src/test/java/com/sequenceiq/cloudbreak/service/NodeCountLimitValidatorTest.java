package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.cloud.PricingCache;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.conf.LimitConfiguration;
import com.sequenceiq.cloudbreak.conf.PrimaryGatewayRequirement;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.projection.StackInstanceCount;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class NodeCountLimitValidatorTest {

    private static final int NODE_COUNT_LIMIT_100 = 100;

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:accountId:environment:envCrn1";

    private static final String ACCOUNT_ID = "accountId";

    private static final String US_WEST_1 = "us-west-1";

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private LimitConfiguration nodeCountLimitConfiguration;

    @Mock
    private Map<CloudPlatform, PricingCache> pricingCacheMap;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private CredentialToExtendedCloudCredentialConverter credentialToExtendedCloudCredentialConverter;

    @InjectMocks
    private NodeCountLimitValidator underTest;

    @Mock
    private StackView stackView;

    @Mock
    private InstanceMetadataView primaryGateway;

    @Mock
    private InstanceGroup gatewayGroup;

    @Mock
    private Template template;

    @Mock
    private PricingCache pricingCache;

    @Mock
    private ExtendedCloudCredential extendedCloudCredential;

    @Mock
    private Credential credential;

    @BeforeEach
    public void setUp() {
        lenient().when(nodeCountLimitConfiguration.getNodeCountLimit(any())).thenReturn(NODE_COUNT_LIMIT_100);
    }

    @Test
    public void testUpscaleValidationSucceeds() {
        when(instanceMetaDataService.countByStackId(anyLong())).thenReturn(count(10));
        underTest.validateScale(stackView, 1, ACCOUNT_ID);
        when(instanceMetaDataService.countByStackId(anyLong())).thenReturn(count(99));
        underTest.validateScale(stackView, 1, ACCOUNT_ID);

        verify(instanceMetaDataService, times(2)).countByStackId(anyLong());
    }

    @Test
    public void testUpscaleValidationFailsBecauseTargetNodeCountExceedsLimit() {
        when(instanceMetaDataService.countByStackId(anyLong())).thenReturn(count(99));

        assertThrows(BadRequestException.class, () -> underTest.validateScale(stackView, 2, "accountId"),
                "The maximum count of nodes for this cluster cannot be higher than 500");
    }

    @Test
    public void testUpscaleValidationOnPrimaryGateway() {
        PrimaryGatewayRequirement primaryGatewayRequirement = new PrimaryGatewayRequirement();
        primaryGatewayRequirement.setMinCpu(16);
        primaryGatewayRequirement.setMinMemory(128);
        primaryGatewayRequirement.setNodeCount(50);
        primaryGatewayRequirement.setRecommendedInstance(Map.of("AWS", "r5.8xlarge"));
        when(nodeCountLimitConfiguration.getPrimaryGatewayRequirement(50)).thenReturn(Optional.of(primaryGatewayRequirement));
        when(nodeCountLimitConfiguration.getPrimaryGatewayRequirement(49)).thenReturn(Optional.empty());
        when(stackView.getId()).thenReturn(1L);
        when(stackView.getCloudPlatform()).thenReturn("AWS");
        when(stackView.getRegion()).thenReturn(US_WEST_1);
        when(stackView.getEnvironmentCrn()).thenReturn(ENV_CRN);
        when(instanceMetaDataService.getPrimaryGatewayInstanceMetadataOrError(anyLong())).thenReturn(primaryGateway);
        when(primaryGateway.getInstanceId()).thenReturn("instance-1");
        when(instanceGroupService.getPrimaryGatewayInstanceGroupByStackId(anyLong())).thenReturn(gatewayGroup);
        when(gatewayGroup.getTemplate()).thenReturn(template);
        when(template.getInstanceType()).thenReturn("m5.xlarge");
        when(instanceMetaDataService.countByStackId(anyLong())).thenReturn(count(48));
        when(pricingCacheMap.containsKey(CloudPlatform.AWS)).thenReturn(true);
        when(pricingCacheMap.get(CloudPlatform.AWS)).thenReturn(pricingCache);
        when(credentialClientService.getByEnvironmentCrn(any())).thenReturn(credential);
        when(credentialToExtendedCloudCredentialConverter.convert(credential)).thenReturn(extendedCloudCredential);
        when(pricingCache.getCpuCountForInstanceType(US_WEST_1, "m5.xlarge", extendedCloudCredential)).thenReturn(Optional.of(4));
        when(pricingCache.getMemoryForInstanceType(US_WEST_1, "m5.xlarge", extendedCloudCredential)).thenReturn(Optional.of(16));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.validateScale(stackView, 2, ACCOUNT_ID));

        assertEquals(
                "Primary gateway instance 'instance-1' doesn't have enough cpu and memory resources to handle a cluster with 50 nodes. " +
                        "The current instance type m5.xlarge has 4 vCPU and 16 GB memory, " +
                        "the recommended instance type is r5.8xlarge which has 16 vCPU and 128 GB memory. " +
                        "In order to proceed please scale vertically the primary gateway and execute a repair on it to make the changes take effect.",
                exception.getMessage());

        assertDoesNotThrow(() -> underTest.validateScale(stackView, 1, ACCOUNT_ID));
    }

    @Test
    public void testProvisionValidationOnPrimaryGateway() {
        PrimaryGatewayRequirement primaryGatewayRequirement = new PrimaryGatewayRequirement();
        primaryGatewayRequirement.setMinCpu(16);
        primaryGatewayRequirement.setMinMemory(128);
        primaryGatewayRequirement.setNodeCount(50);
        primaryGatewayRequirement.setRecommendedInstance(Map.of("AWS", "r5.8xlarge"));
        when(nodeCountLimitConfiguration.getPrimaryGatewayRequirement(50)).thenReturn(Optional.of(primaryGatewayRequirement));
        when(nodeCountLimitConfiguration.getPrimaryGatewayRequirement(49)).thenReturn(Optional.empty());
        when(pricingCacheMap.containsKey(CloudPlatform.AWS)).thenReturn(true);
        when(pricingCacheMap.get(CloudPlatform.AWS)).thenReturn(pricingCache);
        when(credentialClientService.getByEnvironmentCrn(any())).thenReturn(credential);
        when(credentialToExtendedCloudCredentialConverter.convert(credential)).thenReturn(extendedCloudCredential);
        when(pricingCache.getCpuCountForInstanceType(US_WEST_1, "m5.xlarge", extendedCloudCredential)).thenReturn(Optional.of(4));
        when(pricingCache.getMemoryForInstanceType(US_WEST_1, "m5.xlarge", extendedCloudCredential)).thenReturn(Optional.of(16));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.validateProvision(stackV4Request(50), US_WEST_1));

        assertEquals(
                "Primary gateway instance type doesn't have enough cpu and memory resources to handle a cluster with 50 nodes. " +
                        "The current instance type m5.xlarge has 4 vCPU and 16 GB memory, " +
                        "the recommended instance type is r5.8xlarge which has 16 vCPU and 128 GB memory. " +
                        "In order to proceed please scale vertically the primary gateway and execute a repair on it to make the changes take effect.",
                exception.getMessage());

        assertDoesNotThrow(() -> underTest.validateProvision(stackV4Request(49), US_WEST_1));
    }

    @Test
    public void testDownscaleValidation() {
        underTest.validateScale(stackView, -1, ACCOUNT_ID);
        verifyNoInteractions(instanceMetaDataService);
    }

    @Test
    public void testProvisionValidation() {
        when(nodeCountLimitConfiguration.getNodeCountLimit(any())).thenReturn(NODE_COUNT_LIMIT_100);
        underTest.validateProvision(stackV4Request(NODE_COUNT_LIMIT_100), "region");
        underTest.validateProvision(stackV4Request(99), "region");
        assertThrows(BadRequestException.class, () -> underTest.validateProvision(stackV4Request(101), "region"),
                "The maximum count of nodes for this cluster cannot be higher than 500");
    }

    private StackInstanceCount count(int count) {
        return new StackInstanceCount() {
            @Override
            public Long getStackId() {
                return 1L;
            }

            @Override
            public Integer getInstanceCount() {
                return count;
            }
        };

    }

    private StackV4Request stackV4Request(int nodeCount) {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setCloudPlatform(CloudPlatform.AWS);
        InstanceGroupV4Request instanceGroupV4Request = new InstanceGroupV4Request();
        instanceGroupV4Request.setNodeCount(nodeCount);
        instanceGroupV4Request.setType(InstanceGroupType.GATEWAY);
        InstanceTemplateV4Request instanceTemplateV4Request = new InstanceTemplateV4Request();
        instanceTemplateV4Request.setInstanceType("m5.xlarge");
        instanceGroupV4Request.setTemplate(instanceTemplateV4Request);
        stackV4Request.setInstanceGroups(Lists.newArrayList(instanceGroupV4Request));
        stackV4Request.setEnvironmentCrn(ENV_CRN);
        return stackV4Request;
    }
}
