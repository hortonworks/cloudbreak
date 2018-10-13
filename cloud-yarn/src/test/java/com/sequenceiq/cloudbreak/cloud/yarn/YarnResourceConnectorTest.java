package com.sequenceiq.cloudbreak.cloud.yarn;

import static com.sequenceiq.cloudbreak.common.type.ResourceType.YARN_APPLICATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.yarn.auth.YarnClientUtil;
import com.sequenceiq.cloudbreak.cloud.yarn.client.YarnClient;
import com.sequenceiq.cloudbreak.cloud.yarn.client.api.YarnResourceConstants;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.ConfigFile;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.ConfigFileType;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.YarnComponent;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.ApplicationDetailRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.CreateApplicationRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ApplicationDetailResponse;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ResponseContext;
import com.sequenceiq.cloudbreak.common.type.CommonStatus;

@RunWith(MockitoJUnitRunner.class)
public class YarnResourceConnectorTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final String YARN_QUEUE = "YARN_QUEUE";

    private static final Integer YARN_LIFE_TIME = 2;

    private static final String LOGIN_USER_NAME = "lognUserName";

    private static final String PUBLIC_KEY = "publicKey";

    private static final String IMAGE_NAME = "imageName";

    private static final String USER_DATA = "userData";

    private static final String USER_DATA_BASE64 = Base64.getEncoder().encodeToString(USER_DATA.getBytes());

    @InjectMocks
    private YarnResourceConnector underTest;

    @Mock
    private AuthenticatedContext authenticatedContextMock;

    @Mock
    private CloudStack stackMock;

    @Mock
    private PersistenceNotifier persistenceNotifierMock;

    @Mock
    private Image imageMock;

    @Mock
    private YarnClientUtil yarnClientUtilMock;

    @Mock
    private YarnClient yarnClientMock;

    @Mock
    private CloudInstance cloudInstanceMock;

    @Mock
    private InstanceTemplate instanceTemplateMock;

    @Test
    public void testLaunchWithStackDefaultParameters() throws Exception {
        String defaultQueue = "QUEUE";
        int defaultLifeTime = 1;

        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", 20);
        ReflectionTestUtils.setField(underTest, "defaultQueue", defaultQueue);
        ReflectionTestUtils.setField(underTest, "defaultLifeTime", defaultLifeTime);

        ArgumentCaptor<CreateApplicationRequest> createRequestCaptor = ArgumentCaptor.forClass(CreateApplicationRequest.class);
        ArgumentCaptor<ApplicationDetailRequest> requestCaptor = ArgumentCaptor.forClass(ApplicationDetailRequest.class);

        setUpHappyPath(createRequestCaptor, requestCaptor);

        underTest.launch(authenticatedContextMock,
                stackMock, persistenceNotifierMock, AdjustmentType.EXACT, Long.MAX_VALUE);

        CreateApplicationRequest caputeredCreateRequest = createRequestCaptor.getValue();
        assertEquals("name-1-horton", caputeredCreateRequest.getName());
        assertEquals(defaultQueue, caputeredCreateRequest.getQueue());
        assertEquals(defaultLifeTime, caputeredCreateRequest.getLifetime());
    }

    @Test
    public void testLaunch() throws Exception {
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", 3);

        ArgumentCaptor<CreateApplicationRequest> createRequestCaptor = ArgumentCaptor.forClass(CreateApplicationRequest.class);
        ArgumentCaptor<ApplicationDetailRequest> requestCaptor = ArgumentCaptor.forClass(ApplicationDetailRequest.class);

        setUpHappyPath(createRequestCaptor, requestCaptor);
        setUpStackParameters();

        List<Group> groupList = IntStream.range(0, 2).mapToObj(this::createGroup).collect(Collectors.toList());
        when(stackMock.getGroups()).thenReturn(groupList);
        when(stackMock.getLoginUserName()).thenReturn(LOGIN_USER_NAME);
        when(stackMock.getPublicKey()).thenReturn(PUBLIC_KEY);
        when(imageMock.getUserDataByType(InstanceGroupType.CORE)).thenReturn(USER_DATA);
        when(cloudInstanceMock.getTemplate()).thenReturn(instanceTemplateMock);
        when(instanceTemplateMock.getParameter(PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS, Integer.class)).thenReturn(2);
        when(instanceTemplateMock.getParameter(PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY, Integer.class)).thenReturn(4096);

        List<CloudResourceStatus> cloudResourceStatusList = underTest.launch(authenticatedContextMock,
                stackMock, persistenceNotifierMock, AdjustmentType.EXACT, Long.MAX_VALUE);

        verify(persistenceNotifierMock, times(1)).notifyAllocation(any(), any());

        String expectedAppName = "n-1";
        assertCreateRequest(createRequestCaptor, groupList, expectedAppName);
        ApplicationDetailRequest capturedRequest = requestCaptor.getValue();
        assertEquals(expectedAppName, capturedRequest.getName());
        assertCloudResourceStatusList(cloudResourceStatusList, expectedAppName);
    }

    private void assertCloudResourceStatusList(List<CloudResourceStatus> cloudResourceStatusList, String expectedAppName) {
        assertEquals(1L, cloudResourceStatusList.size());
        assertEquals(YARN_APPLICATION, cloudResourceStatusList.get(0).getCloudResource().getType());
        assertEquals(CommonStatus.CREATED, cloudResourceStatusList.get(0).getCloudResource().getStatus());
        assertEquals(expectedAppName, cloudResourceStatusList.get(0).getCloudResource().getName());
        assertEquals(ResourceStatus.CREATED, cloudResourceStatusList.get(0).getStatus());
    }

    private Group createGroup(Integer groupNum) {
        String name = "group_" + groupNum;
        InstanceGroupType type = InstanceGroupType.CORE;
        Collection<CloudInstance> instances = Lists.newArrayList(cloudInstanceMock, cloudInstanceMock);

        return new Group(name, type, instances, null, null, null, null, null, 50);
    }

    private void setUpStackParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(YarnConstants.YARN_QUEUE_PARAMETER, YARN_QUEUE);
        parameters.put(YarnConstants.YARN_LIFETIME_PARAMETER, YARN_LIFE_TIME.toString());
        when(stackMock.getParameters()).thenReturn(parameters);
    }

    private void assertCreateRequest(ArgumentCaptor<CreateApplicationRequest> createRequestCaptor, List<Group> groupList, String expectedAppName) {
        CreateApplicationRequest caputeredCreateRequest = createRequestCaptor.getValue();
        assertEquals(expectedAppName, caputeredCreateRequest.getName());
        assertEquals(YARN_QUEUE, caputeredCreateRequest.getQueue());
        assertEquals(groupList.size(), caputeredCreateRequest.getComponents().size());
        for (int i = 0; i < groupList.size(); i++) {
            Group group = groupList.get(i);
            YarnComponent yarnComponent = caputeredCreateRequest.getComponents().get(i);

            assertEquals(IMAGE_NAME, yarnComponent.getArtifact().getId());
            assertEquals("DOCKER", yarnComponent.getArtifact().getType());
            assertEquals(group.getName(), yarnComponent.getName());
            assertEquals(group.getInstancesSize(), Integer.valueOf(yarnComponent.getNumberOfContainers()));
            String expectedLaunchCommand = "/bootstrap/start-systemd '" + USER_DATA_BASE64 + "' '" + LOGIN_USER_NAME + "' '" + PUBLIC_KEY + '\'';
            assertEquals(expectedLaunchCommand, yarnComponent.getLaunchCommand());
            assertTrue(yarnComponent.getDependencies().isEmpty());

            assertResource(group, yarnComponent);
            assertTrue(yarnComponent.getRunPrivilegedContainer());

            assertNull(yarnComponent.getConfiguration().getEnv());

            assertConfigurationFiles(yarnComponent);
            assertConfigurationProperties(i, yarnComponent);

        }
        assertEquals(YARN_LIFE_TIME.intValue(), caputeredCreateRequest.getLifetime());
    }

    private void assertResource(Group group, YarnComponent yarnComponent) {
        InstanceTemplate instanceTemplate = group.getReferenceInstanceConfiguration().getTemplate();
        Integer cpuParam = instanceTemplate.getParameter(PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS, Integer.class);
        assertEquals(cpuParam, Integer.valueOf(yarnComponent.getResource().getCpus()));
        Integer memoryParam = instanceTemplate.getParameter(PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY, Integer.class);
        assertEquals(memoryParam, Integer.valueOf(yarnComponent.getResource().getMemory()));
    }

    private void assertConfigurationFiles(YarnComponent yarnComponent) {
        assertEquals(1L, yarnComponent.getConfiguration().getFiles().size());
        ConfigFile configFile = yarnComponent.getConfiguration().getFiles().get(0);
        assertEquals(ConfigFileType.PROPERTIES.name(), configFile.getType());
        assertEquals("/etc/cloudbreak-config.props", configFile.getDestFile());
        assertEquals("cb-conf", configFile.getSrcFile());
        assertNull(configFile.getProps());
    }

    private void assertConfigurationProperties(int groupNum, YarnComponent yarnComponent) {
        Map<String, String> properties = yarnComponent.getConfiguration().getProperties();
        assertEquals(5L, properties.size());
        assertEquals("true", properties.get("conf.cb-conf.per.component"));
        assertEquals('\'' + USER_DATA_BASE64 + '\'', properties.get("site.cb-conf.userData"));
        assertEquals('\'' + LOGIN_USER_NAME + '\'', properties.get("site.cb-conf.sshUser"));
        assertEquals("'group_" + groupNum + '\'', properties.get("site.cb-conf.groupname"));
        assertEquals('\'' + PUBLIC_KEY + '\'', properties.get("site.cb-conf.sshPubKey"));
    }

    private void setUpHappyPath(ArgumentCaptor<CreateApplicationRequest> createRequestCaptor, ArgumentCaptor<ApplicationDetailRequest> requestCaptor)
            throws MalformedURLException {
        when(authenticatedContextMock.getCloudContext()).thenReturn(new CloudContext(1L, "name",
                "platform", USER_ID, WORKSPACE_ID));
        when(stackMock.getImage()).thenReturn(imageMock);
        when(imageMock.getImageName()).thenReturn(IMAGE_NAME);
        List<Group> groupList = Collections.emptyList();
        when(stackMock.getGroups()).thenReturn(groupList);
        Map<String, String> parameters = new HashMap<>();
        when(stackMock.getParameters()).thenReturn(parameters);

        when(yarnClientUtilMock.createYarnClient(authenticatedContextMock)).thenReturn(yarnClientMock);

        ResponseContext responseContext = createResponseContext(YarnResourceConstants.HTTP_SUCCESS);
        when(yarnClientMock.createApplication(createRequestCaptor.capture())).thenReturn(responseContext);

        when(yarnClientMock.getApplicationDetail(requestCaptor.capture()))
                .thenReturn(createResponseContext(YarnResourceConstants.HTTP_NOT_FOUND))
                .thenReturn(responseContext);
    }

    private ResponseContext createResponseContext(int statusCode) {
        ResponseContext responseContext = new ResponseContext();
        responseContext.setStatusCode(statusCode);
        if (statusCode == YarnResourceConstants.HTTP_SUCCESS) {
            ApplicationDetailResponse responseObject = new ApplicationDetailResponse();
            responseObject.setState("READY");
            responseContext.setResponseObject(responseObject);
        }
        return responseContext;
    }

    @Test
    public void testLaunchApplicationAlreadyCreated() throws Exception {
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", 8);

        when(authenticatedContextMock.getCloudContext()).thenReturn(new CloudContext(1L, "name", "platform",
                USER_ID, WORKSPACE_ID));

        when(yarnClientUtilMock.createYarnClient(authenticatedContextMock)).thenReturn(yarnClientMock);
        ArgumentCaptor<ApplicationDetailRequest> requestCaptor = ArgumentCaptor.forClass(ApplicationDetailRequest.class);
        when(yarnClientMock.getApplicationDetail(requestCaptor.capture()))
                .thenReturn(createResponseContext(YarnResourceConstants.HTTP_SUCCESS));

        List<CloudResourceStatus> cloudResourceStatusList = underTest.launch(authenticatedContextMock,
                stackMock, persistenceNotifierMock, AdjustmentType.EXACT, Long.MAX_VALUE);

        ApplicationDetailRequest capturedRequest = requestCaptor.getValue();
        String expectedAppName = "n-1-hort";
        assertEquals(expectedAppName, capturedRequest.getName());
        assertCloudResourceStatusList(cloudResourceStatusList, "n-1-hort");
    }
}
