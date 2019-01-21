package com.sequenceiq.cloudbreak.service.decorator;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType.GATEWAY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.model.SpecialParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.cloud.model.Orchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.stack.StackRequestValidator;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.StackParameterService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

public class StackDecoratorTest {

    private static final String MISCONFIGURED_STACK_FOR_SHARED_SERVICE = "Shared service stack should contains both Hive RDS and Ranger "
            + "RDS and a properly configured LDAP also. One of them may be missing";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private StackDecorator underTest;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private StackRequestValidator stackValidator;

    @Mock
    private StackParameterService stackParameterService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private NetworkService networkService;

    @Mock
    private TemplateService templateService;

    @Mock
    private SecurityGroupService securityGroupService;

    @Mock
    private TemplateDecorator templateDecorator;

    @Mock
    private TemplateValidator templateValidator;

    @Mock
    private ConversionService conversionService;

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private FlexSubscriptionService flexSubscriptionService;

    @Mock
    private CloudParameterCache cloudParameterCache;

    @Mock
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Mock
    private Stack subject;

    @Mock
    private StackV4Request request;

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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Ignore
    public void testDecorateWhenMethodCalledThenExactlyOneSharedServiceConfigProviderCallShouldHappen() {
        Stack expected = new Stack();
        when(subject.getCredential()).thenReturn(mock(Credential.class));
        when(cloudParameterCache.getPlatformParameters()).thenReturn(platformParametersMap);
        when(platformParametersMap.get(any(Platform.class))).thenReturn(pps);
        when(pps.specialParameters()).thenReturn(specialParameters);
        when(specialParameters.getSpecialParameters()).thenReturn(specialParametersMap);
        when(specialParametersMap.get(anyString())).thenReturn(false);
        when(cloudParameterService.getOrchestrators()).thenReturn(platformOrchestrators);
        when(platformOrchestrators.getDefaults()).thenReturn(defaultOrchestrator);
        when(defaultOrchestrator.get(any(Platform.class))).thenReturn(orchestrator);
        when(conversionService.convert(any(InstanceGroup.class), any())).thenReturn(instanceGroupParameterRequest);
        when(subject.getFullNodeCount()).thenReturn(2);
        when(subject.getInstanceGroups()).thenReturn(createInstanceGroups(GATEWAY));

        Stack result = underTest.decorate(subject, request, user, workspace);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testDecoratorWhenClusterToAttachIsNotNullAndAllSharedServiceRequirementMeetsThenEverythingShouldGoFine() {
        ClusterV4Request clusterRequest = mock(ClusterV4Request.class);
        Set<DatabaseV4Request> rdsConfigRequests = createRdsConfigRequests("hive", "ranger");
        when(subject.getCredential()).thenReturn(mock(Credential.class));
        when(cloudParameterCache.getPlatformParameters()).thenReturn(platformParametersMap);
        when(platformParametersMap.get(any(Platform.class))).thenReturn(pps);
        when(pps.specialParameters()).thenReturn(specialParameters);
        when(specialParameters.getSpecialParameters()).thenReturn(specialParametersMap);
        when(specialParametersMap.get(anyString())).thenReturn(false);
        when(cloudParameterService.getOrchestrators()).thenReturn(platformOrchestrators);
        when(platformOrchestrators.getDefaults()).thenReturn(defaultOrchestrator);
        when(defaultOrchestrator.get(any(Platform.class))).thenReturn(orchestrator);
        when(conversionService.convert(any(InstanceGroup.class), any())).thenReturn(instanceGroupParameterRequest);
        when(subject.getFullNodeCount()).thenReturn(2);
        when(subject.getInstanceGroups()).thenReturn(createInstanceGroups(GATEWAY));
        when(request.getCluster()).thenReturn(clusterRequest);

        underTest.decorate(subject, request, user, workspace);
    }

    @Test
    public void testDecoratorWhenClusterToAttachIsNotNullAndThereIsNoLdapConfiguredButRangerAndHiveRdsHaveThenExceptionWouldCome() {
        ClusterV4Request clusterRequest = mock(ClusterV4Request.class);
        Set<DatabaseV4Request> rdsConfigRequests = createRdsConfigRequests("hive", "ranger");
        when(subject.getCredential()).thenReturn(mock(Credential.class));
        when(cloudParameterCache.getPlatformParameters()).thenReturn(platformParametersMap);
        when(platformParametersMap.get(any(Platform.class))).thenReturn(pps);
        when(pps.specialParameters()).thenReturn(specialParameters);
        when(specialParameters.getSpecialParameters()).thenReturn(specialParametersMap);
        when(specialParametersMap.get(anyString())).thenReturn(false);
        when(cloudParameterService.getOrchestrators()).thenReturn(platformOrchestrators);
        when(platformOrchestrators.getDefaults()).thenReturn(defaultOrchestrator);
        when(defaultOrchestrator.get(any(Platform.class))).thenReturn(orchestrator);
        when(conversionService.convert(any(InstanceGroup.class), any())).thenReturn(instanceGroupParameterRequest);
        when(subject.getFullNodeCount()).thenReturn(2);
        when(subject.getInstanceGroups()).thenReturn(createInstanceGroups(GATEWAY));
        when(request.getCluster()).thenReturn(clusterRequest);

        underTest.decorate(subject, request, user, workspace);
    }

    @Test
    public void testDecoratorWhenClusterToAttachIsNotNullAndThereIsAnLdapAndRangerRdsConfiguredButHiveRdsDoesNotThenExceptionWouldCome() {
        ClusterV4Request clusterRequest = mock(ClusterV4Request.class);
        Set<DatabaseV4Request> rdsConfigRequests = createRdsConfigRequests("ranger");
        when(subject.getCredential()).thenReturn(mock(Credential.class));
        when(cloudParameterCache.getPlatformParameters()).thenReturn(platformParametersMap);
        when(platformParametersMap.get(any(Platform.class))).thenReturn(pps);
        when(pps.specialParameters()).thenReturn(specialParameters);
        when(specialParameters.getSpecialParameters()).thenReturn(specialParametersMap);
        when(specialParametersMap.get(anyString())).thenReturn(false);
        when(cloudParameterService.getOrchestrators()).thenReturn(platformOrchestrators);
        when(platformOrchestrators.getDefaults()).thenReturn(defaultOrchestrator);
        when(defaultOrchestrator.get(any(Platform.class))).thenReturn(orchestrator);
        when(conversionService.convert(any(InstanceGroup.class), any())).thenReturn(instanceGroupParameterRequest);
        when(subject.getFullNodeCount()).thenReturn(2);
        when(subject.getInstanceGroups()).thenReturn(createInstanceGroups(GATEWAY));
        when(request.getCluster()).thenReturn(clusterRequest);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(MISCONFIGURED_STACK_FOR_SHARED_SERVICE);

        underTest.decorate(subject, request, user, workspace);
    }

    @Test
    public void testDecoratorWhenClusterToAttachIsNotNullAndThereIsAnLdapAndHiveRdsConfiguredButRangerRdsDoesNotThenExceptionWouldCome() {
        ClusterV4Request clusterRequest = mock(ClusterV4Request.class);
        Set<DatabaseV4Request> rdsConfigRequests = createRdsConfigRequests("hive");
        when(subject.getCredential()).thenReturn(mock(Credential.class));
        when(cloudParameterCache.getPlatformParameters()).thenReturn(platformParametersMap);
        when(platformParametersMap.get(any(Platform.class))).thenReturn(pps);
        when(pps.specialParameters()).thenReturn(specialParameters);
        when(specialParameters.getSpecialParameters()).thenReturn(specialParametersMap);
        when(specialParametersMap.get(anyString())).thenReturn(false);
        when(cloudParameterService.getOrchestrators()).thenReturn(platformOrchestrators);
        when(platformOrchestrators.getDefaults()).thenReturn(defaultOrchestrator);
        when(defaultOrchestrator.get(any(Platform.class))).thenReturn(orchestrator);
        when(conversionService.convert(any(InstanceGroup.class), any())).thenReturn(instanceGroupParameterRequest);
        when(subject.getFullNodeCount()).thenReturn(2);
        when(subject.getInstanceGroups()).thenReturn(createInstanceGroups(GATEWAY));
        when(request.getCluster()).thenReturn(clusterRequest);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(MISCONFIGURED_STACK_FOR_SHARED_SERVICE);

        underTest.decorate(subject, request, user, workspace);
    }

    @Test
    public void testDecoratorWhenClusterToAttachIsNotNullAndThereIsAnLdapConfiguredButNoRangerRdsOrHiveRdsConfiguredThenExceptionWouldCome() {
        ClusterV4Request clusterRequest = mock(ClusterV4Request.class);
        Set<DatabaseV4Request> rdsConfigRequests = createRdsConfigRequests("some other value");
        when(subject.getCredential()).thenReturn(mock(Credential.class));
        when(cloudParameterCache.getPlatformParameters()).thenReturn(platformParametersMap);
        when(platformParametersMap.get(any(Platform.class))).thenReturn(pps);
        when(pps.specialParameters()).thenReturn(specialParameters);
        when(specialParameters.getSpecialParameters()).thenReturn(specialParametersMap);
        when(specialParametersMap.get(anyString())).thenReturn(false);
        when(cloudParameterService.getOrchestrators()).thenReturn(platformOrchestrators);
        when(platformOrchestrators.getDefaults()).thenReturn(defaultOrchestrator);
        when(defaultOrchestrator.get(any(Platform.class))).thenReturn(orchestrator);
        when(conversionService.convert(any(InstanceGroup.class), any())).thenReturn(instanceGroupParameterRequest);
        when(subject.getFullNodeCount()).thenReturn(2);
        when(subject.getInstanceGroups()).thenReturn(createInstanceGroups(GATEWAY));
        when(request.getCluster()).thenReturn(clusterRequest);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(MISCONFIGURED_STACK_FOR_SHARED_SERVICE);

        underTest.decorate(subject, request, user, workspace);
    }

    private Set<DatabaseV4Request> createRdsConfigRequests(String... types) {
        Set<DatabaseV4Request> requests = new LinkedHashSet<>(types.length);
        for (int i = 0; i < types.length; i++) {
            DatabaseV4Request request = new DatabaseV4Request();
            request.setType(types[i]);
            request.setName(String.format("RdsConfigRequest_%d", i));
            request.setConnectionURL(String.format("0.0.0.%d", i));
            request.setConnectionUserName("username");
            request.setConnectionPassword("password");
            requests.add(request);
        }
        return requests;
    }

    private Set<InstanceGroup> createInstanceGroups(InstanceGroupType... types) {
        Set<InstanceGroup> groups = new LinkedHashSet<>(types.length);
        for (InstanceGroupType type : types) {
            InstanceGroup group = new InstanceGroup();
            group.setInstanceGroupType(type);
            group.setGroupName("name");
            groups.add(group);
        }
        return groups;
    }

}