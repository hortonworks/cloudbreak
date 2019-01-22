package com.sequenceiq.cloudbreak.converter.stack;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.stackauthentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackV4RequestToStackConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.service.stack.StackParameterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class StackV4RequestToStackConverterTest extends AbstractJsonConverterTest<StackV4Request> {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private StackV4RequestToStackConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private StackParameterService stackParameterService;

    @Mock
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private PreferencesService preferencesService;

    @Mock
    private DefaultCostTaggingService defaultCostTaggingService;

    @Mock
    private StackService stackService;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private CloudbreakUser cloudbreakUser;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
    }

    @Test
    public void testConvert() throws CloudbreakException {
        initMocks();
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-2");
        given(orchestratorTypeResolver.resolveType(any(Orchestrator.class))).willReturn(OrchestratorType.HOST);
        given(defaultCostTaggingService.prepareDefaultTags(any(CloudbreakUser.class), anyMap(), anyString())).willReturn(new HashMap<>());
        // WHEN
        Stack stack = underTest.convert(getRequest("stack.json"));
        // THEN
        assertAllFieldsNotNull(
                stack,
                Arrays.asList("description", "statusReason", "cluster", "credential", "gatewayPort", "template", "network", "securityConfig", "securityGroup",
                        "version", "created", "platformVariant", "cloudPlatform", "saltPassword", "stackTemplate", "flexSubscription", "datalakeId",
                        "customHostname", "customDomain", "clusterNameAsSubdomain", "hostgroupNameAsHostname", "loginUserName", "parameters",
                        "rootVolumeSize", "creator", "environment", "terminated", "datalakeResourceId", "type"));
        assertEquals("YARN", stack.getRegion());
    }

    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    @Test
    public void testConvertWithNoGateway() throws CloudbreakException {
        initMocks();
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-2");
        // WHEN
        try {
            Stack stack = underTest.convert(getRequest("stack.json"));
        } catch (BadRequestException e) {
            //THEN
            assertEquals("Ambari server must be specified", e.getMessage());
        }
    }

    @Test
    public void testConvertWithLoginUserName() throws CloudbreakException {
        initMocks();
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-2");
        given(defaultCostTaggingService.prepareDefaultTags(any(CloudbreakUser.class), anyMap(), anyString())).willReturn(new HashMap<>());
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("You can not modify the default user!");
        // WHEN
        Stack stack = underTest.convert(getRequest("stack-with-loginusername.json"));
        // THEN
        assertAllFieldsNotNull(
                stack,
                Arrays.asList("description", "statusReason", "cluster", "credential", "gatewayPort", "template", "network", "securityConfig", "securityGroup",
                        "version", "created", "platformVariant", "cloudPlatform", "saltPassword", "stackTemplate", "flexSubscription", "datalakeId",
                        "customHostname", "customDomain", "clusterNameAsSubdomain", "hostgroupNameAsHostname", "loginUserName", "rootVolumeSize"));
        assertEquals("eu-west-1", stack.getRegion());
    }

    @Test
    public void testForNoRegionAndNoDefaultRegion() throws CloudbreakException {
        initMocks();
        given(orchestratorTypeResolver.resolveType(any(String.class))).willReturn(OrchestratorType.HOST);
        given(defaultCostTaggingService.prepareDefaultTags(any(CloudbreakUser.class), anyMap(), anyString())).willReturn(new HashMap<>());
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("No default region is specified. Region cannot be empty.");

        // WHEN
        StackV4Request stackRequest = getRequest("stack.json");
        stackRequest.getEnvironment().getPlacement().setRegion(null);
        underTest.convert(stackRequest);
    }

    @Test
    public void testConvertSharedServicePreparateWhenSourceTagsAndClusterToAttachFieldsAreNullThenDatalakeIdShouldNotBeSet() throws CloudbreakException {
        StackV4Request source = getRequest("stack.json");
        source.getTags().setApplicationTags(null);
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);

        //GIVEN
        given(orchestratorTypeResolver.resolveType(any(String.class))).willReturn(OrchestratorType.HOST);
        given(conversionService.convert(any(InstanceGroupV4Request.class), eq(InstanceGroup.class))).willReturn(instanceGroup);

        //WHEN
        Stack result = underTest.convert(source);

        //THEN
        Assert.assertNull(result.getDatalakeId());
    }

    @Test
    public void testConvertSharedServicePreparateWhenSourceTagsNotNullButNoDatalakeIdEntryInItAndClusterToAttachIsNullThenDatalakeIdShouldNotBeSet()
            throws CloudbreakException {
        StackV4Request source = getRequest("stack.json");
        source.getTags().setApplicationTags(Collections.emptyMap());
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);

        //GIVEN
        given(orchestratorTypeResolver.resolveType(any(String.class))).willReturn(OrchestratorType.HOST);
        given(conversionService.convert(any(InstanceGroupV4Request.class), eq(InstanceGroup.class))).willReturn(instanceGroup);

        //WHEN
        Stack result = underTest.convert(source);

        //THEN
        Assert.assertNull(result.getDatalakeId());
    }

    @Test
    public void testConvertSharedServicePreparateWhenThereIsNoDatalakeIdInSourceTagsButClusterToAttachIsNotNullThenThisDataShoudlBeTheDatalakeId()
            throws CloudbreakException {
        Long expectedDataLakeId = 1L;
        StackV4Request source = getRequest("stack.json");
        source.getTags().setApplicationTags(Collections.emptyMap());
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);

        //GIVEN
        given(orchestratorTypeResolver.resolveType(any(String.class))).willReturn(OrchestratorType.HOST);
        given(conversionService.convert(any(InstanceGroupV4Request.class), eq(InstanceGroup.class))).willReturn(instanceGroup);

        //WHEN
        Stack result = underTest.convert(source);

        //THEN
        assertEquals(expectedDataLakeId, result.getDatalakeId());
    }

    @Test
    public void testConvertSharedServicePreparateWhenThereIsNoClusterToAttachButApplicationTagsContansDatalakeIdKeyThenItsValueShouldBeSetAsDatalakeId()
            throws CloudbreakException {
        String expectedDataLakeId = "1";
        StackV4Request source = getRequest("stack.json");
        Map<String, String> applicationTags = new LinkedHashMap<>(1);
        applicationTags.put("datalakeId", expectedDataLakeId);
        source.getTags().setApplicationTags(applicationTags);
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);

        //GIVEN
        given(orchestratorTypeResolver.resolveType(any(String.class))).willReturn(OrchestratorType.HOST);
        given(conversionService.convert(any(InstanceGroupV4Request.class), eq(InstanceGroup.class))).willReturn(instanceGroup);

        //WHEN
        Stack result = underTest.convert(source);

        //THEN
        assertEquals(Long.valueOf(expectedDataLakeId), result.getDatalakeId());
    }

    @Override
    public Class<StackV4Request> getRequestClass() {
        return StackV4Request.class;
    }

    private void initMocks() throws CloudbreakException {
        // GIVEN
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<>(Collections.singletonList(instanceGroup)));
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<>(Collections.singletonList(instanceGroup)));

        given(conversionService.convert(any(StackAuthenticationV4Request.class), eq(StackAuthentication.class))).willReturn(new StackAuthentication());
        given(conversionService.convert(any(InstanceGroupV4Request.class), eq(InstanceGroup.class))).willReturn(instanceGroup);
        given(orchestratorTypeResolver.resolveType(any(String.class))).willReturn(OrchestratorType.HOST);
    }
}
