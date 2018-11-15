package com.sequenceiq.cloudbreak.converter.v2;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.FailurePolicyRequest;
import com.sequenceiq.cloudbreak.api.model.NetworkRequest;
import com.sequenceiq.cloudbreak.api.model.SharedServiceRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.GeneralSettings;
import com.sequenceiq.cloudbreak.api.model.v2.ImageSettings;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.PlacementSettings;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.Tags;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

public class StackV2RequestToStackRequestConverterTest {

    private static final NetworkRequest NETWORK_REQUEST = new NetworkRequest();

    private static final String CLOUD_PLATFORM = "somePlatform";

    private static final int GENERAL_TEST_QUANTITY = 2;

    private static final String TEST_OWNER_EMAIL = "owneremail@email.com";

    @InjectMocks
    private StackV2RequestToStackRequestConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private EnvironmentViewService environmentService;

    @Mock
    private StackService stackService;

    @Mock
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private Credential credential;

    @Mock
    private EnvironmentView environmentView;

    @Mock
    private CloudbreakUser cbUser;

    @Mock
    private User user;

    @Mock
    private Workspace workspace;

    @Before
    public void setUp() {
        credentialService = mock(CredentialService.class);
        MockitoAnnotations.initMocks(this);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cbUser);
        when(userService.getOrCreate(eq(cbUser))).thenReturn(user);
        when(workspaceService.get(anyLong(), eq(user))).thenReturn(workspace);
        when(credentialService.getByNameForWorkspace(any(), any(Workspace.class))).thenReturn(credential);
        when(environmentService.getByNameForWorkspace(any(), any(Workspace.class))).thenReturn(environmentView);
        when(credential.cloudPlatform()).thenReturn(CLOUD_PLATFORM);
        when(environmentView.getCloudPlatform()).thenReturn(CLOUD_PLATFORM);
        when(conversionService.convert(any(NetworkV2Request.class), any())).thenReturn(NETWORK_REQUEST);
    }

    @Test
    public void testConvertWhenEveryLogicIndependentDataHasSet() {
        StackV2Request source = createStackV2Request();
        makeCbUserReturnNullsAsUsedFields();
        when(conversionService.convert(source.getNetwork(), NetworkRequest.class)).thenReturn(NETWORK_REQUEST);
        StackRequest result = underTest.convert(source);

        Assert.assertEquals(source.getGeneral().getName(), result.getName());
        Assert.assertEquals(source.getPlatformVariant(), result.getPlatformVariant());
        Assert.assertEquals(source.getAmbariVersion(), result.getAmbariVersion());
        Assert.assertEquals(source.getHdpVersion(), result.getHdpVersion());
        Assert.assertTrue(result.getParameters().isEmpty());
        Assert.assertTrue(result.getInstanceGroups().isEmpty());
        Assert.assertEquals(source.getFailurePolicy(), result.getFailurePolicy());
        Assert.assertEquals(source.getStackAuthentication(), result.getStackAuthentication());
        Assert.assertEquals(NETWORK_REQUEST, result.getNetwork());
        Assert.assertEquals("SALT", result.getOrchestrator().getType());
        Assert.assertEquals(source.getFlexId(), result.getFlexId());
        Assert.assertEquals(source.getGeneral().getCredentialName(), result.getCredentialName());
        Assert.assertEquals(source.getGeneral().getEnvironmentName(), result.getEnvironment());
        Assert.assertEquals(CLOUD_PLATFORM, result.getCloudPlatform());
        verify(conversionService, times(1)).convert(any(), any());
        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
    }

    @Test
    public void testConvertIfSourceParametersFieldIsNullThenNullShuldBePlacedInTheReturnObjectAsTheParametersField() {
        StackV2Request source = createStackV2Request();
        makeCbUserReturnNullsAsUsedFields();
        source.setParameters(null);

        StackRequest result = underTest.convert(source);

        Assert.assertNull(result.getParameters());
        verify(conversionService, times(1)).convert(any(), any());
    }

    @Test
    public void testConvertIfTheProvidedParametersFieldHasContentThenTheSameKeyValuePairsShouldBeInTheReturnObjectParametersField() {
        StackV2Request source = createStackV2Request();
        makeCbUserReturnNullsAsUsedFields();
        Map<String, String> parameters = createMap();
        source.setParameters(parameters);

        StackRequest result = underTest.convert(source);

        Assert.assertFalse(result.getParameters().isEmpty());
        Assert.assertEquals(parameters.size(), result.getParameters().size());
        parameters.forEach((s, s2) -> {
            Assert.assertTrue(result.getParameters().containsKey(s));
            Assert.assertEquals(s2, result.getParameters().get(s));
        });
        verify(conversionService, times(1)).convert(any(), any());
        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
    }

    @Test
    public void testConvertWhenPlacementIsNotNullThenRelatedFieldsShouldBeSet() {
        StackV2Request source = createStackV2Request();
        makeCbUserReturnNullsAsUsedFields();
        PlacementSettings placementSettings = new PlacementSettings();
        placementSettings.setAvailabilityZone("zome");
        placementSettings.setRegion("region");
        source.setPlacement(placementSettings);

        StackRequest result = underTest.convert(source);

        Assert.assertEquals(placementSettings.getAvailabilityZone(), result.getAvailabilityZone());
        Assert.assertEquals(placementSettings.getRegion(), result.getRegion());
        verify(conversionService, times(1)).convert(any(), any());
        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
    }

    @Test
    public void testConvertWhenPlacementIsNullThenRelatedFieldsShouldNotBeSet() {
        StackV2Request source = createStackV2Request();
        makeCbUserReturnNullsAsUsedFields();
        source.setPlacement(null);

        StackRequest result = underTest.convert(source);

        Assert.assertNull(result.getAvailabilityZone());
        Assert.assertNull(result.getRegion());
        verify(conversionService, times(1)).convert(any(), any());
        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
    }

    @Test
    public void testConvertWhenTagsIsNullThenTheRelatedFieldsShouldNotBeSet() {
        StackV2Request source = createStackV2Request();
        makeCbUserReturnNullsAsUsedFields();
        source.setTags(null);

        StackRequest result = underTest.convert(source);

        Assert.assertNotNull(result.getApplicationTags());
        Assert.assertTrue(result.getApplicationTags().isEmpty());
        Assert.assertNotNull(result.getDefaultTags());
        Assert.assertTrue(result.getDefaultTags().isEmpty());
        Assert.assertNotNull(result.getUserDefinedTags());
        Assert.assertTrue(result.getUserDefinedTags().isEmpty());
        verify(conversionService, times(1)).convert(any(), any());
        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
    }

    @Test
    public void testConvertWhenTagsHasValuesThenRelatedFieldsShouldBeSet() {
        StackV2Request source = createStackV2Request();
        makeCbUserReturnNullsAsUsedFields();
        Tags tags = new Tags();
        tags.setApplicationTags(createMap());
        tags.setDefaultTags(createMap());
        tags.setUserDefinedTags(createMap());
        source.setTags(tags);

        StackRequest result = underTest.convert(source);

        Assert.assertNotNull(result.getApplicationTags());
        Assert.assertFalse(result.getApplicationTags().isEmpty());
        tags.getApplicationTags().forEach((s, s2) -> {
            Assert.assertTrue(result.getApplicationTags().containsKey(s));
            Assert.assertEquals(s2, result.getApplicationTags().get(s));
        });

        Assert.assertNotNull(result.getDefaultTags());
        Assert.assertFalse(result.getDefaultTags().isEmpty());
        tags.getDefaultTags().forEach((s, s2) -> {
            Assert.assertTrue(result.getDefaultTags().containsKey(s));
            Assert.assertEquals(s2, result.getDefaultTags().get(s));
        });

        Assert.assertNotNull(result.getUserDefinedTags());
        Assert.assertFalse(result.getUserDefinedTags().isEmpty());
        tags.getUserDefinedTags().forEach((s, s2) -> {
            Assert.assertTrue(result.getUserDefinedTags().containsKey(s));
            Assert.assertEquals(s2, result.getUserDefinedTags().get(s));
        });
        verify(conversionService, times(1)).convert(any(), any());
        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
    }

    @Test
    public void testConvertWhenThereAreSomeDataInInstanceGroupsThenTheExpectedOnesShouldBeInTheResultInstance() {
        StackV2Request source = createStackV2Request();
        makeCbUserReturnNullsAsUsedFields();
        List<InstanceGroupV2Request> instanceGroupV2Requests = createInstanceGroupV2Request();
        List<InstanceGroupRequest> instanceGroupRequest = createInstanceGroupRequest();
        when(conversionService.convert(instanceGroupV2Requests.get(0), InstanceGroupRequest.class)).thenReturn(instanceGroupRequest.get(0));
        when(conversionService.convert(instanceGroupV2Requests.get(1), InstanceGroupRequest.class)).thenReturn(instanceGroupRequest.get(1));
        source.setInstanceGroups(instanceGroupV2Requests);

        StackRequest result = underTest.convert(source);

        Assert.assertNotNull(result.getInstanceGroups());
        Assert.assertEquals(source.getInstanceGroups().size(), result.getInstanceGroups().size());
        instanceGroupRequest.forEach(request -> Assert.assertTrue(result.getInstanceGroups().contains(request)));

        verify(conversionService, times(instanceGroupV2Requests.size() + 1)).convert(any(), any());
        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
    }

    @Test
    public void testConvertWhenImageSettingsIsNullThenRelatedFieldsShouldNotBeSet() {
        StackV2Request source = createStackV2Request();
        makeCbUserReturnNullsAsUsedFields();
        source.setImageSettings(null);

        StackRequest result = underTest.convert(source);

        Assert.assertNull(result.getImageCatalog());
        Assert.assertNull(result.getImageId());
        verify(conversionService, times(1)).convert(any(), any());
        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
    }

    @Test
    public void testConvertWhenImageSettingsIsNotNullThenRelatedFieldsShouldBeSet() {
        StackV2Request source = createStackV2Request();
        makeCbUserReturnNullsAsUsedFields();
        ImageSettings imageSettings = new ImageSettings();
        imageSettings.setImageCatalog("imageCatalog");
        imageSettings.setImageId("1234");
        source.setImageSettings(imageSettings);

        StackRequest result = underTest.convert(source);

        Assert.assertEquals(source.getImageSettings().getImageCatalog(), result.getImageCatalog());
        Assert.assertEquals(source.getImageSettings().getImageId(), result.getImageId());
        verify(conversionService, times(1)).convert(any(), any());
        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
    }

    @Test
    public void testConvertWhenOwnerIsNullThenAuthenticatedUserServiceShouldProvideTheValue() {
        StackV2Request source = createStackV2Request();

        underTest.convert(source);

        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
        verify(conversionService, times(1)).convert(any(), any());
    }

    @Test
    public void testConvertWhenOwnerIsEmptyThenAuthenticatedUserServiceShouldProvideTheValue() {
        StackV2Request source = createStackV2Request();

        underTest.convert(source);

        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
        verify(conversionService, times(1)).convert(any(), any());
    }

    @Test
    public void testConvertWhenOwnerIsNotEmptyThenTheProvidedValueShouldBeSet() {
        StackV2Request source = createStackV2Request();

        underTest.convert(source);

        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
        verify(conversionService, times(1)).convert(any(), any());
    }

    @Test
    public void testConvertWhenAccountIsNullThenAuthenticatedUserServiceShouldProvideTheValue() {
        StackV2Request source = createStackV2Request();

        underTest.convert(source);

        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
        verify(conversionService, times(1)).convert(any(), any());
    }

    @Test
    public void testConvertWhenAccountIsEmptyThenAuthenticatedUserServiceShouldProvideTheValue() {
        StackV2Request source = createStackV2Request();

        underTest.convert(source);

        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
        verify(conversionService, times(1)).convert(any(), any());
    }

    @Test
    public void testConvertWhenAccountIsNotEmptyThenTheProvidedValueShouldBeSet() {
        StackV2Request source = createStackV2Request();

        underTest.convert(source);

        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
        verify(conversionService, times(1)).convert(any(), any());
    }

    @Test
    public void testConvertWhenOwnerEmailIsNullThenAuthenticatedUserServiceShouldProvideTheValue() {
        StackV2Request source = createStackV2Request();
        when(cbUser.getUsername()).thenReturn(TEST_OWNER_EMAIL);

        underTest.convert(source);

        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
        verify(conversionService, times(1)).convert(any(), any());
    }

    @Test
    public void testConvertWhenOwnerEmailIsEmptyThenAuthenticatedUserServiceShouldProvideTheValue() {
        StackV2Request source = createStackV2Request();
        when(cbUser.getUsername()).thenReturn(TEST_OWNER_EMAIL);

        underTest.convert(source);

        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
        verify(conversionService, times(1)).convert(any(), any());
    }

    @Test
    public void testConvertWhenOwnerEmailIsNotEmptyThenTheProvidedValueShouldBeSet() {
        StackV2Request source = createStackV2Request();

        underTest.convert(source);

        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
        verify(conversionService, times(1)).convert(any(), any());
    }

    @Test
    public void testConvertWhenClusterIsNullThenThereShouldBeNoConversionAndSharedServiceConfigProviderCall() {
        StackV2Request source = createStackV2Request();
        source.setCluster(null);

        StackRequest result = underTest.convert(source);

        Assert.assertNull(result.getClusterRequest());
        verify(sharedServiceConfigProvider, times(0)).isConfigured(any());
    }

    @Test
    public void testConvertWhenClusterIsNotNullButTheInstanceGroupsAreEmptyAndTheClusterIsNotConfiguredBySharedServiceConfigProvider() {
        StackV2Request source = createStackV2Request();
        source.setCluster(createClusterV2Request());
        ClusterRequest convertedRequest = createClusterRequest();
        when(conversionService.convert(source.getCluster(), ClusterRequest.class)).thenReturn(convertedRequest);
        when(sharedServiceConfigProvider.isConfigured(source.getCluster())).thenReturn(false);

        StackRequest result = underTest.convert(source);

        Assert.assertEquals(convertedRequest, result.getClusterRequest());
        Assert.assertEquals(source.getGeneral().getName(), result.getClusterRequest().getName());
        verify(conversionService, times(2)).convert(any(), any());
        verify(sharedServiceConfigProvider, times(1)).isConfigured(source.getCluster());
    }

    @Test
    public void testConvertWhenClusterIsNotNullAndTheInstanceGroupIsNotEmptyButTheClusterIsNotConfiguredBySharedServiceConfigProvider() {
        StackV2Request source = createStackV2Request();
        source.setCluster(createClusterV2Request());
        List<InstanceGroupV2Request> instanceGroupV2Requests = createInstanceGroupV2Request();
        List<HostGroupRequest> hostGroupRequest = createHostGroupRequests();
        List<InstanceGroupRequest> instanceGroupRequest = createInstanceGroupRequest();
        when(conversionService.convert(instanceGroupV2Requests.get(0), InstanceGroupRequest.class)).thenReturn(instanceGroupRequest.get(0));
        when(conversionService.convert(instanceGroupV2Requests.get(0), HostGroupRequest.class)).thenReturn(hostGroupRequest.get(0));
        when(conversionService.convert(instanceGroupV2Requests.get(1), InstanceGroupRequest.class)).thenReturn(instanceGroupRequest.get(1));
        when(conversionService.convert(instanceGroupV2Requests.get(1), HostGroupRequest.class)).thenReturn(hostGroupRequest.get(1));
        source.setInstanceGroups(instanceGroupV2Requests);
        when(sharedServiceConfigProvider.isConfigured(source.getCluster())).thenReturn(false);
        ClusterRequest convertedRequest = createClusterRequest();
        when(conversionService.convert(source.getCluster(), ClusterRequest.class)).thenReturn(convertedRequest);

        StackRequest result = underTest.convert(source);

        Assert.assertEquals(convertedRequest, result.getClusterRequest());
        Assert.assertEquals(source.getGeneral().getName(), result.getClusterRequest().getName());
        verify(conversionService, times(hostGroupRequest.size() + instanceGroupRequest.size() + 2)).convert(any(), any());
        verify(sharedServiceConfigProvider, times(1)).isConfigured(source.getCluster());
    }

    @Test
    public void testConvertWhenClusterIsNotNullAndIsConfiguredByTheSharedServiceProvider() {
        StackV2Request source = createStackV2Request();
        source.setCluster(createClusterV2Request());
        ClusterRequest convertedRequest = createClusterRequest();
        when(conversionService.convert(source.getCluster(), ClusterRequest.class)).thenReturn(convertedRequest);
        when(sharedServiceConfigProvider.isConfigured(source.getCluster())).thenReturn(true);
        Stack mockStack = mock(Stack.class);
        when(stackService.getByNameInWorkspace(eq(source.getCluster().getSharedService().getSharedCluster()), anyLong())).thenReturn(mockStack);
        Long id = 1L;
        when(mockStack.getId()).thenReturn(id);

        StackRequest result = underTest.convert(source);

        Assert.assertEquals(convertedRequest, result.getClusterRequest());
        Assert.assertEquals(source.getGeneral().getName(), result.getClusterRequest().getName());
        Assert.assertEquals(id, result.getClusterToAttach());
        verify(conversionService, times(2)).convert(any(), any());
        verify(sharedServiceConfigProvider, times(1)).isConfigured(source.getCluster());
        verify(stackService, times(1)).getByNameInWorkspace(eq(source.getCluster().getSharedService().getSharedCluster()), anyLong());
        verify(mockStack, times(1)).getId();
        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
    }

    private StackV2Request createStackV2Request() {
        StackV2Request request = new StackV2Request();
        request.setGeneral(createGeneralSettings());
        request.setPlatformVariant("some platform variant");
        request.setAmbariVersion("1.0");
        request.setHdpVersion("3.1");
        request.setInstanceGroups(Collections.emptyList());
        request.setFailurePolicy(new FailurePolicyRequest());
        request.setStackAuthentication(new StackAuthenticationRequest());
        request.setNetwork(new NetworkV2Request());
        request.setFlexId(1L);
        return request;
    }

    private List<InstanceGroupV2Request> createInstanceGroupV2Request() {
        List<InstanceGroupV2Request> requests = new ArrayList<>(2);
        for (int i = 0; i < 2; i++) {
            requests.add(new InstanceGroupV2Request());
        }
        return requests;
    }

    private List<InstanceGroupRequest> createInstanceGroupRequest() {
        List<InstanceGroupRequest> requests = new ArrayList<>(2);
        for (int i = 0; i < 2; i++) {
            requests.add(new InstanceGroupRequest());
        }
        return requests;
    }

    private List<HostGroupRequest> createHostGroupRequests() {
        List<HostGroupRequest> requests = new ArrayList<>(2);
        for (int i = 0; i < 2; i++) {
            requests.add(new HostGroupRequest());
        }
        return requests;
    }

    private GeneralSettings createGeneralSettings() {
        GeneralSettings generalSettings = new GeneralSettings();
        generalSettings.setName("some name");
        generalSettings.setCredentialName("credential name");
        generalSettings.setEnvironmentName("environment name");
        return generalSettings;
    }

    private Map<String, String> createMap() {
        Map<String, String> params = new LinkedHashMap<>(GENERAL_TEST_QUANTITY);
        for (int i = 0; i < GENERAL_TEST_QUANTITY; i++) {
            params.put(String.valueOf(i), String.format("some_value_%s", String.valueOf(i)));
        }
        return params;
    }

    private void makeCbUserReturnNullsAsUsedFields() {
        when(cbUser.getUserId()).thenReturn(null);
        when(cbUser.getUsername()).thenReturn(null);
    }

    private ClusterV2Request createClusterV2Request() {
        ClusterV2Request request = new ClusterV2Request();
        SharedServiceRequest sharedServiceRequest = new SharedServiceRequest();
        sharedServiceRequest.setSharedCluster("shared cluster");
        request.setSharedService(sharedServiceRequest);
        return request;
    }

    private ClusterRequest createClusterRequest() {
        ClusterRequest request = new ClusterRequest();
        request.setHostGroups(new LinkedHashSet<>());
        return request;
    }

}