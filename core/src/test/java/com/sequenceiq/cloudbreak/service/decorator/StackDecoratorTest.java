package com.sequenceiq.cloudbreak.service.decorator;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType.GATEWAY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.api.model.SpecialParameters;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.cloud.model.Orchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.SharedServiceValidator;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

public class StackDecoratorTest {

    private static final String MISCONFIGURED_STACK_FOR_SHARED_SERVICE = "Shared service stack configuration contains some errors";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private StackDecorator underTest;

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
    private ConverterUtil converterUtil;

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private FlexSubscriptionService flexSubscriptionService;

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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        subject = new Stack();
        subject.setCredential(mock(Credential.class));
        subject.setInstanceGroups(createInstanceGroups(GATEWAY));
        when(cloudParameterCache.getPlatformParameters()).thenReturn(platformParametersMap);
        when(platformParametersMap.get(any(Platform.class))).thenReturn(pps);
        when(pps.specialParameters()).thenReturn(specialParameters);
        when(specialParameters.getSpecialParameters()).thenReturn(specialParametersMap);
        when(specialParametersMap.get(anyString())).thenReturn(false);
        when(cloudParameterService.getOrchestrators()).thenReturn(platformOrchestrators);
        when(platformOrchestrators.getDefaults()).thenReturn(defaultOrchestrator);
        when(defaultOrchestrator.get(any(Platform.class))).thenReturn(orchestrator);
        when(converterUtil.convert(any(InstanceGroup.class), eq(InstanceGroupParameterRequest.class))).thenReturn(instanceGroupParameterRequest);
        when(request.getCluster()).thenReturn(clusterRequest);
        when(request.getEnvironment()).thenReturn(environmentSettingsRequest);
        when(environmentSettingsRequest.getCredentialName()).thenReturn("myCredentialName");
        when(sharedServiceValidator.checkSharedServiceStackRequirements(any(StackV4Request.class), any(Workspace.class))).thenReturn(validationResult);
    }

    @Test
    public void testDecorateWhenMethodCalledThenExactlyOneSharedServiceValidatorCallShouldHappen() {
        underTest.decorate(subject, request, user, workspace);

        verify(sharedServiceValidator, times(1)).checkSharedServiceStackRequirements(any(StackV4Request.class), any(Workspace.class));
    }

    @Test
    public void testDecoratorWhenClusterToAttachIsNotNullAndAllSharedServiceRequirementMeetsThenEverythingShouldGoFine() {
        SharedServiceV4Request sharedServiceV4Request = new SharedServiceV4Request();
        sharedServiceV4Request.setDatalakeName("dlname");
        when(request.getSharedService()).thenReturn(sharedServiceV4Request);
        when(clusterRequest.getDatabases()).thenReturn(Set.of("db1", "db2"));

        underTest.decorate(subject, request, user, workspace);
    }

    @Test
    public void testDecoratorWhenClusterToAttachIsNotNullAndThereIsNoLdapConfiguredButRangerAndHiveRdsHaveThenExceptionWouldCome() {
        when(validationResult.hasError()).thenReturn(Boolean.TRUE);
        when(validationResult.getFormattedErrors()).thenReturn(MISCONFIGURED_STACK_FOR_SHARED_SERVICE);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(MISCONFIGURED_STACK_FOR_SHARED_SERVICE);

        underTest.decorate(subject, request, user, workspace);
    }

    private Set<InstanceGroup> createInstanceGroups(InstanceGroupType... types) {
        Set<InstanceGroup> groups = new LinkedHashSet<>(types.length);
        for (InstanceGroupType type : types) {
            InstanceGroup group = mock(InstanceGroup.class);
            when(group.getInstanceGroupType()).thenReturn(type);
            when(group.getGroupName()).thenReturn("name");
            when(group.getNodeCount()).thenReturn(2);
            groups.add(group);
        }
        return groups;
    }

}