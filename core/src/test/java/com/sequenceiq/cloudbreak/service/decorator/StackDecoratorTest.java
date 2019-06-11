package com.sequenceiq.cloudbreak.service.decorator;

import static com.sequenceiq.cloudbreak.common.type.InstanceGroupType.GATEWAY;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.cloud.model.Orchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;
import com.sequenceiq.cloudbreak.cloud.model.SpecialParameters;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.SharedServiceValidator;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

public class StackDecoratorTest {

    private static final String MISCONFIGURED_STACK_FOR_SHARED_SERVICE = "Shared service stack configuration contains some errors";

    private static final long CREDENTIAL_ID = 1L;

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private StackDecorator underTest;

    @Mock
    private CredentialClientService credentialClientService;

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
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Before
    public void setUp() {
        String credentialName = "credentialName";
        MockitoAnnotations.initMocks(this);
        subject = new Stack();
        subject.setCredentialCrn("aCredentialCRN");
        subject.setEnvironmentCrn("envCrn");
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
        when(environmentSettingsRequest.getCredentialName()).thenReturn(credentialName);
        when(sharedServiceValidator.checkSharedServiceStackRequirements(any(StackV4Request.class), any(Workspace.class))).thenReturn(validationResult);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCredentialName(credentialName);
        when(environmentClientService.getByName(anyString())).thenReturn(environmentResponse);
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);
        when(credentialClientService.getByCrn(anyString())).thenReturn(Credential.builder().cloudPlatform(CloudPlatform.MOCK.name()).build());
        when(credentialClientService.getByName(anyString())).thenReturn(Credential.builder().cloudPlatform(CloudPlatform.MOCK.name()).build());
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
