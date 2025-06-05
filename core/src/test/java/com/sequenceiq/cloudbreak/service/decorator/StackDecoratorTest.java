package com.sequenceiq.cloudbreak.service.decorator;

import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
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
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterCache;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupToInstanceGroupParameterRequestConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.AvailabilityZone;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
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

@ExtendWith(MockitoExtension.class)
public class StackDecoratorTest {
    private static final String ACCOUNT_ID = "cloudera";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:test@cloudera.com";

    private static final String MISCONFIGURED_STACK_FOR_SHARED_SERVICE = "Shared service stack configuration contains some errors";

    private static final Set<String> EXPECTED_AZS = Set.of("availabilityZone1", "availabilityZone2");

    private static final String CREATOR_CLIENT_HEADER_NAME = "user-agent";

    private static final String CREATOR_CLIENT_HEADER_VALUE = "CDPTFPROVIDER/dev";

    private static final String CREATOR_CLIENT_FALLBACK_HEADER_NAME = "cdp-caller-id";

    private static final String CREATOR_CLIENT_DEFAULT_VALUE = "No Info";

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
    private EnvironmentService environmentClientService;

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

    @Captor
    private ArgumentCaptor<Set<AvailabilityZone>> azSetCaptor;

    private DetailedEnvironmentResponse environmentResponse;

    @BeforeEach
    public void setUp() {
        String credentialName = "credentialName";
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
        lenient().when(request.getCluster()).thenReturn(clusterRequest);
        lenient().when(sharedServiceValidator.checkSharedServiceStackRequirements(any(StackV4Request.class), any(Workspace.class)))
                .thenReturn(validationResult);
        environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCredential(credentialResponse);
        CompactRegionResponse crr = new CompactRegionResponse();
        crr.setNames(Lists.newArrayList("region"));
        environmentResponse.setRegions(crr);
        EnvironmentNetworkResponse enr = new EnvironmentNetworkResponse();
        Map<String, CloudSubnet> subnetmetas = Maps.newHashMap("subnet",
                new CloudSubnet.Builder()
                        .id("id")
                        .name("name")
                        .availabilityZone("availabilityzone")
                        .cidr("cidr")
                        .build()
        );
        enr.setSubnetMetas(subnetmetas);
        environmentResponse.setNetwork(enr);
        environmentResponse.setAzure(AzureEnvironmentParameters
                .builder().withAzureResourceGroup(AzureResourceGroup
                        .builder().withResourceGroupUsage(ResourceGroupUsage.SINGLE).withName("resource-group").build()).build());
        lenient().when(request.getCloudPlatform()).thenReturn(CloudPlatform.AZURE);
        lenient().when(environmentClientService.getByName(anyString())).thenReturn(environmentResponse);
        Credential credential = Credential.builder().cloudPlatform(CloudPlatform.MOCK.name()).build();
        lenient().when(credentialClientService.getByCrn(anyString())).thenReturn(credential);
        lenient().when(credentialClientService.getByName(anyString())).thenReturn(credential);
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
                () -> underTest.decorate(environmentResponse, subject, request, user, workspace));

        verify(sharedServiceValidator, times(1)).checkSharedServiceStackRequirements(any(StackV4Request.class), any(Workspace.class));
    }

    @Test
    public void testDecorateWhenMethodCalledWithMultipleGatewayGroupThenShouldThrowBadRequestException() {
        Set<InstanceGroup> instanceGroups = createInstanceGroups(GATEWAY, GATEWAY);
        subject.setInstanceGroups(instanceGroups);
        assertThrows(BadRequestException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.decorate(environmentResponse, subject, request, user, workspace)));
    }

    @Test
    public void testDecoratorWhenClusterToAttachIsNotNullAndAllSharedServiceRequirementMeetsThenEverythingShouldGoFine() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.decorate(environmentResponse, subject, request, user, workspace));
        assertEquals("resource-group", subject.getParameters().get(PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER));
        assertEquals("SINGLE", subject.getParameters().get(PlatformParametersConsts.RESOURCE_GROUP_USAGE_PARAMETER));

        for (InstanceGroup group : subject.getInstanceGroups()) {
            verify(group).setAvailabilityZones(azSetCaptor.capture());
            Set<String> capturedAzSet = azSetCaptor.getValue().stream().map(AvailabilityZone::getAvailabilityZone).collect(Collectors.toSet());
            assertEquals(EXPECTED_AZS, capturedAzSet);
        }

    }

    @Test
    public void testDecoratorWhenClusterToAttachIsNotNullAndThereIsNoLdapConfiguredButRangerAndHiveRdsHaveThenExceptionWouldCome() {
        when(validationResult.hasError()).thenReturn(Boolean.TRUE);
        when(validationResult.getFormattedErrors()).thenReturn(MISCONFIGURED_STACK_FOR_SHARED_SERVICE);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.decorate(environmentResponse, subject, request, user, workspace)));
        assertEquals(MISCONFIGURED_STACK_FOR_SHARED_SERVICE, badRequestException.getMessage());
    }

    @Test
    void testDecoratorWhenCreatorClientPrimaryHeaderIsPresent() {
        setUpServletRequestWithHeaderAndValue(Optional.of(CREATOR_CLIENT_HEADER_NAME), CREATOR_CLIENT_HEADER_VALUE);

        ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.decorate(environmentResponse, subject, request, user, workspace));


        assertEquals(CREATOR_CLIENT_HEADER_VALUE, subject.getCreatorClient());
    }

    @Test
    void testDecoratorWhenCreatorClientPrimaryHeaderIsNotPresentButSecondaryIs() {
        setUpServletRequestWithHeaderAndValue(Optional.of(CREATOR_CLIENT_FALLBACK_HEADER_NAME), CREATOR_CLIENT_HEADER_VALUE);

        ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.decorate(environmentResponse, subject, request, user, workspace));


        assertEquals(CREATOR_CLIENT_HEADER_VALUE, subject.getCreatorClient());
    }

    @Test
    void testDecoratorWhenNeitherPrimaryNorSecondaryCreatorClientHeaderIsPresent() {
        setUpServletRequestWithHeaderAndValue(Optional.empty(), null);

        ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.decorate(environmentResponse, subject, request, user, workspace));

        assertEquals(CREATOR_CLIENT_DEFAULT_VALUE, subject.getCreatorClient());
    }

    private void setUpServletRequestWithHeaderAndValue(Optional<String> headerName, String headerValue) {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getHeader(headerName.orElse(any()))).thenReturn(headerValue);
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        when(attributes.getRequest()).thenReturn(httpServletRequest);
        RequestContextHolder.setRequestAttributes(attributes);
    }

    private Set<InstanceGroup> createInstanceGroups(InstanceGroupType... types) {
        Set<InstanceGroup> groups = new LinkedHashSet<>(types.length);
        int i = 0;
        for (InstanceGroupType type : types) {
            InstanceGroup group = mock(InstanceGroup.class, Answers.RETURNS_DEEP_STUBS);
            when(group.getInstanceGroupType()).thenReturn(type);
            when(group.getAvailabilityZones()).thenReturn(null);
            when(group.getGroupName()).thenReturn("name" + i);
            lenient().when(group.getNodeCount()).thenReturn(2);
            Json instanceGroupNetworkAttributes = new Json(Map.of(NetworkConstants.AVAILABILITY_ZONES, EXPECTED_AZS));
            when(group.getInstanceGroupNetwork().getAttributes()).thenReturn(instanceGroupNetworkAttributes);
            groups.add(group);
        }
        return groups;
    }
}
