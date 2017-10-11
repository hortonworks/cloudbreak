package com.sequenceiq.cloudbreak.converter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.controller.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesService;
import com.sequenceiq.cloudbreak.service.stack.StackParameterService;

public class JsonToStackConverterTest extends AbstractJsonConverterTest<StackRequest> {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private JsonToStackConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private StackParameterService stackParameterService;

    @Mock
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private AccountPreferencesService accountPreferencesService;

    @Before
    public void setUp() {
        underTest = new JsonToStackConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() throws CloudbreakException {
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);

        // GIVEN
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-2");
        ReflectionTestUtils.setField(underTest, "enableCustomImage", true);
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<>(Collections.singletonList(instanceGroup)));
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(new StackAuthentication())
                .willReturn(instanceGroup)
                .willReturn(instanceGroup)
                .willReturn(new FailurePolicy())
                .willReturn(new Orchestrator());
        //given(stackParameterService.getStackParams(any(IdentityUser.class), any(StackRequest.class))).willReturn(new ArrayList<>());
        given(orchestratorTypeResolver.resolveType(any(Orchestrator.class))).willReturn(OrchestratorType.HOST);
        given(orchestratorTypeResolver.resolveType(any(String.class))).willReturn(OrchestratorType.HOST);
        // WHEN
        Stack stack = underTest.convert(getRequest("stack/stack.json"));
        // THEN
        assertAllFieldsNotNull(
                stack,
                Arrays.asList("description", "statusReason", "cluster", "credential", "gatewayPort", "template", "network", "securityConfig", "securityGroup",
                        "version", "created", "platformVariant", "cloudPlatform", "saltPassword", "stackTemplate", "flexSubscription", "datalakeId",
                        "customHostname", "customDomain", "clusterNameAsSubdomain", "hostgroupNameAsHostname", "loginUserName"));
        Assert.assertEquals("eu-west-1", stack.getRegion());
    }

    @Test
    public void testConvertWithLoginUserName() throws CloudbreakException {
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);

        // GIVEN
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-2");
        ReflectionTestUtils.setField(underTest, "enableCustomImage", true);
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<>(Collections.singletonList(instanceGroup)));
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(new StackAuthentication())
                .willReturn(instanceGroup)
                .willReturn(instanceGroup)
                .willReturn(new FailurePolicy())
                .willReturn(new Orchestrator());
        //given(stackParameterService.getStackParams(any(IdentityUser.class), any(StackRequest.class))).willReturn(new ArrayList<>());
        given(orchestratorTypeResolver.resolveType(any(Orchestrator.class))).willReturn(OrchestratorType.HOST);
        given(orchestratorTypeResolver.resolveType(any(String.class))).willReturn(OrchestratorType.HOST);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("You can not modify the default user!");
        // WHEN
        Stack stack = underTest.convert(getRequest("stack/stack-with-loginusername.json"));
        // THEN
        assertAllFieldsNotNull(
                stack,
                Arrays.asList("description", "statusReason", "cluster", "credential", "gatewayPort", "template", "network", "securityConfig", "securityGroup",
                        "version", "created", "platformVariant", "cloudPlatform", "saltPassword", "stackTemplate", "flexSubscription", "datalakeId",
                        "customHostname", "customDomain", "clusterNameAsSubdomain", "hostgroupNameAsHostname", "loginUserName"));
        Assert.assertEquals("eu-west-1", stack.getRegion());
    }

    @Test
    public void testForNoRegionAndNoDefaultRegion() throws CloudbreakException {
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);

        // GIVEN
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<>(Collections.singletonList(instanceGroup)));
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(instanceGroup)
                .willReturn(instanceGroup)
                .willReturn(new FailurePolicy())
                .willReturn(new Orchestrator());
        //given(stackParameterService.getStackParams(any(IdentityUser.class), any(StackRequest.class))).willReturn(new ArrayList<>());
        given(orchestratorTypeResolver.resolveType(any(Orchestrator.class))).willReturn(OrchestratorType.HOST);
        given(orchestratorTypeResolver.resolveType(any(String.class))).willReturn(OrchestratorType.HOST);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("No default region is specified. Region cannot be empty.");

        // WHEN
        StackRequest stackRequest = getRequest("stack/stack.json");
        OrchestratorRequest orchestratorRequest = new OrchestratorRequest();
        orchestratorRequest.setType("SALT");
        stackRequest.setOrchestrator(orchestratorRequest);
        stackRequest.setRegion(null);
        underTest.convert(stackRequest);
    }

    @Test
    public void testForNoRegionAndDefaultRegion() throws CloudbreakException {
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);
        StackAuthentication stackAuthentication = new StackAuthentication();
        stackAuthentication.setPublicKey("ssh-key dsfsdfsdfsdf");
        stackAuthentication.setLoginUserName("cloudbreak");

        // GIVEN
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-1");
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<>(Collections.singletonList(instanceGroup)));
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(stackAuthentication)
                .willReturn(instanceGroup)
                .willReturn(instanceGroup)
                .willReturn(new FailurePolicy())
                .willReturn(new Orchestrator());
        //given(stackParameterService.getStackParams(any(IdentityUser.class), any(StackRequest.class))).willReturn(new ArrayList<>());
        given(orchestratorTypeResolver.resolveType(any(Orchestrator.class))).willReturn(OrchestratorType.HOST);
        given(orchestratorTypeResolver.resolveType(any(String.class))).willReturn(OrchestratorType.HOST);

        // WHEN
        StackRequest stackRequest = getRequest("stack/stack.json");
        OrchestratorRequest orchestratorRequest = new OrchestratorRequest();
        orchestratorRequest.setType("SALT");
        stackRequest.setOrchestrator(orchestratorRequest);
        stackRequest.setRegion(null);
        Stack stack = underTest.convert(stackRequest);

        // THEN
        Assert.assertEquals("eu-west-1", stack.getRegion());
    }

    @Override
    public Class<StackRequest> getRequestClass() {
        return StackRequest.class;
    }
}
