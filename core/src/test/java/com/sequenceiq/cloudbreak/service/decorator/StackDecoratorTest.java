package com.sequenceiq.cloudbreak.service.decorator;

import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.cloud.model.Orchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;
import com.sequenceiq.cloudbreak.cloud.model.SpecialParameters;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupToInstanceGroupParameterRequestConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.SharedServiceValidator;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

public class StackDecoratorTest {
    private static final String ACCOUNT_ID = "cloudera";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:test@cloudera.com";

    private static final String MISCONFIGURED_STACK_FOR_SHARED_SERVICE = "Shared service stack configuration contains some errors";

    private static final long CREDENTIAL_ID = 1L;

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private StackDecorator underTest;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private CloudParameterCache cloudParameterCache;

    private Stack subject;

    @Mock
    private StackV4Request request;

    @Mock
    private ClusterV4Request clusterRequest;

    @Mock
    private EnvironmentSettingsV4Request environmentSettingsRequest;

    @Mock
    private User user;

    @Mock
    private Workspace workspace;

    @Mock
    private Map<Platform, PlatformParameters> platformParametersMap;

    @Mock
    private PlatformParameters pps;

    @Mock
    private SpecialParameters specialParameters;

    @Mock
    private Map<String, Boolean> specialParametersMap;

    @Mock
    private PlatformOrchestrators platformOrchestrators;

    @Mock
    private Map<Platform, Orchestrator> defaultOrchestrator;

    @Mock
    private Orchestrator orchestrator;

    @Mock
    private InstanceGroupParameterRequest instanceGroupParameterRequest;

    @Mock
    private SharedServiceValidator sharedServiceValidator;

    @Mock
    private ValidationResult validationResult;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Mock
    private CredentialResponse credentialResponse;

    @Mock
    private CredentialConverter credentialConverter;

    @Mock
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Mock
    private InstanceGroupToInstanceGroupParameterRequestConverter instanceGroupToInstanceGroupParameterRequestConverter;

    @Before
    public void setUp() {
        String credentialName = "credentialName";
        MockitoAnnotations.initMocks(this);
        subject = new Stack();
        subject.setEnvironmentCrn("envCrn");
        Set<InstanceGroup> instanceGroups = createInstanceGroups(GATEWAY);
        subject.setInstanceGroups(instanceGroups);
        Cluster cluster = getCluster(instanceGroups);
        subject.setCluster(cluster);
        subject.setCloudPlatform("AZURE");
        subject.setParameters(new HashMap<>());
        when(cloudParameterCache.getPlatformParameters()).thenReturn(platformParametersMap);
        when(platformParametersMap.get(any(Platform.class))).thenReturn(pps);
        when(pps.specialParameters()).thenReturn(specialParameters);
        when(specialParameters.getSpecialParameters()).thenReturn(specialParametersMap);
        when(specialParametersMap.get(anyString())).thenReturn(false);
        when(cloudParameterService.getOrchestrators()).thenReturn(platformOrchestrators);
        when(platformOrchestrators.getDefaults()).thenReturn(defaultOrchestrator);
        when(defaultOrchestrator.get(any(Platform.class))).thenReturn(orchestrator);
        when(instanceGroupToInstanceGroupParameterRequestConverter.convert(any(InstanceGroup.class))).thenReturn(instanceGroupParameterRequest);
        when(request.getCluster()).thenReturn(clusterRequest);
        when(environmentSettingsRequest.getCredentialName()).thenReturn(credentialName);
        when(sharedServiceValidator.checkSharedServiceStackRequirements(any(StackV4Request.class), any(Workspace.class))).thenReturn(validationResult);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCredential(credentialResponse);
        CompactRegionResponse crr = new CompactRegionResponse();
        crr.setNames(Lists.newArrayList("region"));
        environmentResponse.setRegions(crr);
        EnvironmentNetworkResponse enr = new EnvironmentNetworkResponse();
        Map<String, CloudSubnet> subnetmetas = Maps.newHashMap("subnet", new CloudSubnet("id", "name", "availabilityzone", "cidr"));
        enr.setSubnetMetas(subnetmetas);
        environmentResponse.setNetwork(enr);
        environmentResponse.setAzure(AzureEnvironmentParameters
                .builder().withAzureResourceGroup(AzureResourceGroup
                        .builder().withResourceGroupUsage(ResourceGroupUsage.SINGLE).withName("resource-group").build()).build());
        when(request.getCloudPlatform()).thenReturn(CloudPlatform.AZURE);
        when(environmentClientService.getByName(anyString())).thenReturn(environmentResponse);
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);
        Credential credential = Credential.builder().cloudPlatform(CloudPlatform.MOCK.name()).build();
        when(credentialClientService.getByCrn(anyString())).thenReturn(credential);
        when(credentialClientService.getByName(anyString())).thenReturn(credential);
        when(credentialConverter.convert(credentialResponse)).thenReturn(credential);
        ExtendedCloudCredential extendedCloudCredential = mock(ExtendedCloudCredential.class);
        when(extendedCloudCredentialConverter.convert(credential)).thenReturn(extendedCloudCredential);
    }

    private Cluster getCluster(Set<InstanceGroup> instanceGroups) {
        Cluster cluster = new Cluster();
        Set<HostGroup> hostGroups = new HashSet<>();
        for (InstanceGroup instanceGroup : instanceGroups) {
            HostGroup hostGroup = new HostGroup();
            hostGroup.setName(instanceGroup.getGroupName());
            hostGroups.add(hostGroup);
        }
        cluster.setHostGroups(hostGroups);
        return cluster;
    }

    @Test
    public void testDecorateWhenMethodCalledThenExactlyOneSharedServiceValidatorCallShouldHappen() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.decorate(subject, request, user, workspace));

        verify(sharedServiceValidator, times(1)).checkSharedServiceStackRequirements(any(StackV4Request.class), any(Workspace.class));
    }

    @Test(expected = BadRequestException.class)
    public void testDecorateWhenMethodCalledWithMultipleGatewayGroupThenShouldThrowBadRequestException() {
        Set<InstanceGroup> instanceGroups = createInstanceGroups(GATEWAY, GATEWAY);
        subject.setInstanceGroups(instanceGroups);
        ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.decorate(subject, request, user, workspace));
    }

    @Test
    public void testDecoratorWhenClusterToAttachIsNotNullAndAllSharedServiceRequirementMeetsThenEverythingShouldGoFine() {
        SharedServiceV4Request sharedServiceV4Request = new SharedServiceV4Request();
        sharedServiceV4Request.setDatalakeName("dlname");
        when(request.getSharedService()).thenReturn(sharedServiceV4Request);
        when(clusterRequest.getDatabases()).thenReturn(Set.of("db1", "db2"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.decorate(subject, request, user, workspace));
        assertEquals("resource-group", subject.getParameters().get(PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER));
        assertEquals("SINGLE", subject.getParameters().get(PlatformParametersConsts.RESOURCE_GROUP_USAGE_PARAMETER));
    }

    @Test
    public void testDecoratorWhenClusterToAttachIsNotNullAndThereIsNoLdapConfiguredButRangerAndHiveRdsHaveThenExceptionWouldCome() {
        when(validationResult.hasError()).thenReturn(Boolean.TRUE);
        when(validationResult.getFormattedErrors()).thenReturn(MISCONFIGURED_STACK_FOR_SHARED_SERVICE);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(MISCONFIGURED_STACK_FOR_SHARED_SERVICE);

        ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.decorate(subject, request, user, workspace));
    }

    private Set<InstanceGroup> createInstanceGroups(InstanceGroupType... types) {
        Set<InstanceGroup> groups = new LinkedHashSet<>(types.length);
        int i = 0;
        for (InstanceGroupType type : types) {
            InstanceGroup group = mock(InstanceGroup.class);
            when(group.getInstanceGroupType()).thenReturn(type);
            when(group.getGroupName()).thenReturn("name" + i);
            when(group.getNodeCount()).thenReturn(2);
            groups.add(group);
        }
        return groups;
    }
}
