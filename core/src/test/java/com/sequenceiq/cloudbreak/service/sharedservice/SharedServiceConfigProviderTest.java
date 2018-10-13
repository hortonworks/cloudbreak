package com.sequenceiq.cloudbreak.service.sharedservice;

import static com.sequenceiq.cloudbreak.api.model.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.model.ResourceStatus.DEFAULT_DELETED;
import static com.sequenceiq.cloudbreak.api.model.ResourceStatus.USER_MANAGED;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.model.ConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.ConnectedClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.api.model.SharedServiceRequest;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintInputParameters;
import com.sequenceiq.cloudbreak.domain.BlueprintParameter;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public class SharedServiceConfigProviderTest {

    private static final Long TEST_LONG_VALUE = 1L;

    @InjectMocks
    private SharedServiceConfigProvider underTest;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private User user;

    @Mock
    private Workspace workspace;

    @Mock
    private ConnectedClusterRequest connectedClusterRequest;

    @Mock
    private LdapConfig ldapConfig;

    @Mock
    private Blueprint blueprint;

    @Mock
    private Json newInputs;

    @Mock
    private Stack publicStack;

    @Mock
    private Cluster sourceCluster;

    @Mock
    private Cluster publicStackCluster;

    @Mock
    private ConfigsResponse configsResponse;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testIsConfiguredWhenClusterV2RequestHasNoSharedServiceThenFalseExpected() {
        ClusterV2Request request = new ClusterV2Request();
        request.setSharedService(null);

        boolean result = underTest.isConfigured(request);
        Assert.assertFalse(result);
    }

    @Test
    public void testIsConfiguredWhenSharedServiceNotNullButNullSharedClusterStoredThenFalseExpected() {
        ClusterV2Request request = new ClusterV2Request();
        SharedServiceRequest sharedServiceRequest = new SharedServiceRequest();
        sharedServiceRequest.setSharedCluster(null);
        request.setSharedService(sharedServiceRequest);

        boolean result = underTest.isConfigured(request);
        Assert.assertFalse(result);
    }

    @Test
    public void testIsConfiguredWhenSharedServiceNotNullButEmptySharedClusterStoredThenFalseExpected() {
        ClusterV2Request request = new ClusterV2Request();
        SharedServiceRequest sharedServiceRequest = new SharedServiceRequest();
        sharedServiceRequest.setSharedCluster("");
        request.setSharedService(sharedServiceRequest);

        boolean result = underTest.isConfigured(request);
        Assert.assertFalse(result);
    }

    @Test
    public void testIsConfiguredSharedIsNotNullAndHasANotEmptySharedClusterThenTrueExpected() {
        ClusterV2Request request = new ClusterV2Request();
        SharedServiceRequest sharedServiceRequest = new SharedServiceRequest();
        sharedServiceRequest.setSharedCluster("some information");
        request.setSharedService(sharedServiceRequest);

        boolean result = underTest.isConfigured(request);
        Assert.assertTrue(result);
    }

    @Test
    public void testConfigureClusterWhenConnectedClusterRequestIsNullThenOriginalClusterInstanceShouldReturn() {
        Cluster cluster = new Cluster();

        Cluster result = underTest.configureCluster(cluster, null, user, workspace);

        Assert.assertEquals(cluster, result);
    }

    @Test
    public void testConfigureClusterWhenSourceClusterNameDoesNotExistsThenPublicStackInstanceWouldComeFromTheStackServiceGetMethod() throws IOException {
        Cluster requestedCluster = createBarelyConfiguredRequestedCluster();

        when(connectedClusterRequest.getSourceClusterName()).thenReturn(null);
        when(connectedClusterRequest.getSourceClusterId()).thenReturn(TEST_LONG_VALUE);
        when(stackService.getById(TEST_LONG_VALUE)).thenReturn(publicStack);
        when(publicStack.getId()).thenReturn(TEST_LONG_VALUE);
        when(clusterService.getById(TEST_LONG_VALUE)).thenReturn(sourceCluster);
        when(publicStackCluster.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStack.getCluster()).thenReturn(publicStackCluster);
        when(publicStackCluster.getLdapConfig()).thenReturn(ldapConfig);
        when(blueprint.getInputParameters()).thenReturn(null);
        when(clusterService.retrieveOutputs(anyLong(), any())).thenReturn(configsResponse);
        when(configsResponse.getInputs()).thenReturn(Collections.emptySet());
        when(newInputs.get(Map.class)).thenReturn(Collections.emptyMap());

        Cluster result = underTest.configureCluster(requestedCluster, connectedClusterRequest, user, workspace);

        Assert.assertEquals(ldapConfig, result.getLdapConfig());
        Assert.assertTrue(result.getRdsConfigs().isEmpty());
        verify(stackService, times(1)).getById(TEST_LONG_VALUE);
        verify(stackService, times(0)).getByNameInWorkspace(anyString(), anyLong());
    }

    @Test
    public void testConfigureClusterWhenSourceClusterNameExistsThenPublicStackWouldComeFromTheStackServiceGetPublicStack() throws IOException {
        Cluster requestedCluster = createBarelyConfiguredRequestedCluster();

        String clusterName = "some value representing a cluster name";
        when(connectedClusterRequest.getSourceClusterName()).thenReturn(clusterName);
        when(connectedClusterRequest.getSourceClusterId()).thenReturn(TEST_LONG_VALUE);
        when(stackService.getByNameInWorkspace(eq(clusterName), anyLong())).thenReturn(publicStack);
        when(publicStack.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStackCluster.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStack.getCluster()).thenReturn(publicStackCluster);
        when(clusterService.getById(TEST_LONG_VALUE)).thenReturn(sourceCluster);
        when(publicStackCluster.getLdapConfig()).thenReturn(ldapConfig);
        when(blueprint.getInputParameters()).thenReturn(null);
        when(clusterService.retrieveOutputs(anyLong(), any())).thenReturn(configsResponse);
        when(configsResponse.getInputs()).thenReturn(Collections.emptySet());
        when(newInputs.get(Map.class)).thenReturn(Collections.emptyMap());

        Cluster result = underTest.configureCluster(requestedCluster, connectedClusterRequest, user, workspace);

        Assert.assertEquals(ldapConfig, result.getLdapConfig());
        Assert.assertTrue(result.getRdsConfigs().isEmpty());
        verify(stackService, times(0)).getById(TEST_LONG_VALUE);
        verify(stackService, times(1)).getByNameInWorkspace(anyString(), anyLong());
    }

    @Test
    public void testConfigureClusterIfSourceClusterContainsDifferentResourceStatusThenTheDefaultOnesWouldNotBeStoredInTheReturnCluster() throws IOException {
        Cluster requestedCluster = createBarelyConfiguredRequestedCluster();
        Cluster sourceCluster = createBarelyConfiguredRequestedCluster();
        sourceCluster.setRdsConfigs(createRdsConfigs(DEFAULT, DEFAULT_DELETED, USER_MANAGED));

        when(connectedClusterRequest.getSourceClusterName()).thenReturn(null);
        when(connectedClusterRequest.getSourceClusterId()).thenReturn(TEST_LONG_VALUE);
        when(stackService.getById(TEST_LONG_VALUE)).thenReturn(publicStack);
        when(publicStack.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStackCluster.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStack.getCluster()).thenReturn(publicStackCluster);
        when(clusterService.getById(TEST_LONG_VALUE)).thenReturn(sourceCluster);
        when(publicStack.getCluster()).thenReturn(publicStackCluster);
        when(publicStackCluster.getLdapConfig()).thenReturn(ldapConfig);
        when(blueprint.getInputParameters()).thenReturn(null);
        when(clusterService.retrieveOutputs(anyLong(), any())).thenReturn(configsResponse);
        when(configsResponse.getInputs()).thenReturn(Collections.emptySet());
        when(newInputs.get(Map.class)).thenReturn(Collections.emptyMap());

        Cluster result = underTest.configureCluster(requestedCluster, connectedClusterRequest, user, workspace);

        Assert.assertEquals(2L, result.getRdsConfigs().size());
        result.getRdsConfigs().forEach(rdsConfig -> Assert.assertNotEquals(DEFAULT, rdsConfig.getStatus()));
    }

    @Test
    public void testConfigureClusterWhenBlueprintAttributesAreNullThenBlueprintParameterJsonsShouldBeEmpty() throws IOException {
        Cluster requestedCluster = createBarelyConfiguredRequestedCluster();

        when(connectedClusterRequest.getSourceClusterName()).thenReturn(null);
        when(connectedClusterRequest.getSourceClusterId()).thenReturn(TEST_LONG_VALUE);
        when(stackService.getById(TEST_LONG_VALUE)).thenReturn(publicStack);
        when(publicStack.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStackCluster.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStack.getCluster()).thenReturn(publicStackCluster);
        when(clusterService.getById(TEST_LONG_VALUE)).thenReturn(sourceCluster);
        when(publicStackCluster.getLdapConfig()).thenReturn(ldapConfig);
        when(blueprint.getInputParameters()).thenReturn(null);
        when(clusterService.retrieveOutputs(anyLong(), any())).thenReturn(configsResponse);
        when(configsResponse.getInputs()).thenReturn(Collections.emptySet());
        when(newInputs.get(Map.class)).thenReturn(Collections.emptyMap());

        underTest.configureCluster(requestedCluster, connectedClusterRequest, user, workspace);

        verify(clusterService, times(1)).getById(TEST_LONG_VALUE);
        verify(stackService, times(1)).getById(TEST_LONG_VALUE);    }

    @Test
    public void testConfigureClusterWhenBlueprintAttributesisNotNullButItsValueIsNullThenBlueprintParameterJsonsShouldBeEmpty() throws IOException {
        Cluster requestedCluster = createBarelyConfiguredRequestedCluster();
        Json mockBlueprintAttributes = mock(Json.class);

        when(connectedClusterRequest.getSourceClusterName()).thenReturn(null);
        when(connectedClusterRequest.getSourceClusterId()).thenReturn(TEST_LONG_VALUE);
        when(stackService.getById(TEST_LONG_VALUE)).thenReturn(publicStack);
        when(publicStack.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStackCluster.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStack.getCluster()).thenReturn(publicStackCluster);
        when(clusterService.getById(TEST_LONG_VALUE)).thenReturn(sourceCluster);
        when(publicStackCluster.getLdapConfig()).thenReturn(ldapConfig);
        when(blueprint.getInputParameters()).thenReturn(mockBlueprintAttributes);
        when(mockBlueprintAttributes.getValue()).thenReturn(null);
        when(clusterService.retrieveOutputs(anyLong(), any())).thenReturn(configsResponse);
        when(configsResponse.getInputs()).thenReturn(Collections.emptySet());
        when(newInputs.get(Map.class)).thenReturn(Collections.emptyMap());

        underTest.configureCluster(requestedCluster, connectedClusterRequest, user, workspace);

        verify(clusterService, times(1)).getById(TEST_LONG_VALUE);
        verify(stackService, times(1)).getById(TEST_LONG_VALUE);
    }

    @Test
    public void testConfigureClusterWhenBlueprintAttributesisNotNullButItsValueIsEmptyThenBlueprintParameterJsonsShouldBeEmpty() throws IOException {
        Cluster requestedCluster = createBarelyConfiguredRequestedCluster();
        Json mockBlueprintAttributes = mock(Json.class);

        when(connectedClusterRequest.getSourceClusterName()).thenReturn(null);
        when(connectedClusterRequest.getSourceClusterId()).thenReturn(TEST_LONG_VALUE);
        when(stackService.getById(TEST_LONG_VALUE)).thenReturn(publicStack);
        when(publicStack.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStackCluster.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStack.getCluster()).thenReturn(publicStackCluster);
        when(clusterService.getById(TEST_LONG_VALUE)).thenReturn(sourceCluster);
        when(publicStackCluster.getLdapConfig()).thenReturn(ldapConfig);
        when(blueprint.getInputParameters()).thenReturn(mockBlueprintAttributes);
        when(mockBlueprintAttributes.getValue()).thenReturn("");
        when(clusterService.retrieveOutputs(anyLong(), any())).thenReturn(configsResponse);
        when(configsResponse.getInputs()).thenReturn(Collections.emptySet());
        when(newInputs.get(Map.class)).thenReturn(Collections.emptyMap());

        underTest.configureCluster(requestedCluster, connectedClusterRequest, user, workspace);

        verify(clusterService, times(1)).getById(TEST_LONG_VALUE);
        verify(stackService, times(1)).getById(TEST_LONG_VALUE);    }

    @Test
    public void testConfigureClusterWhenBlueprintAttributesisNotNullAndItsValueIsNotEmptyThenBlueprintParameterJsonsShouldNotBeEmpty() throws IOException {
        Cluster requestedCluster = createBarelyConfiguredRequestedCluster();
        Json mockBlueprintAttributes = mock(Json.class);
        BlueprintInputParameters inputParameters = new BlueprintInputParameters();
        inputParameters.setParameters(createBlueprintParameters());

        when(connectedClusterRequest.getSourceClusterName()).thenReturn(null);
        when(connectedClusterRequest.getSourceClusterId()).thenReturn(TEST_LONG_VALUE);
        when(stackService.getById(TEST_LONG_VALUE)).thenReturn(publicStack);
        when(publicStack.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStackCluster.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStack.getCluster()).thenReturn(publicStackCluster);
        when(clusterService.getById(TEST_LONG_VALUE)).thenReturn(sourceCluster);
        when(publicStackCluster.getLdapConfig()).thenReturn(ldapConfig);
        when(blueprint.getInputParameters()).thenReturn(mockBlueprintAttributes);
        when(mockBlueprintAttributes.getValue()).thenReturn("some value which does not empty or null");
        when(mockBlueprintAttributes.get(BlueprintInputParameters.class)).thenReturn(inputParameters);
        when(clusterService.retrieveOutputs(anyLong(), any())).thenReturn(configsResponse);
        when(configsResponse.getInputs()).thenReturn(Collections.emptySet());
        when(newInputs.get(Map.class)).thenReturn(Collections.emptyMap());

        underTest.configureCluster(requestedCluster, connectedClusterRequest, user, workspace);

        verify(clusterService, times(0)).retrieveOutputs(TEST_LONG_VALUE, new HashSet<>());
    }

    private Cluster createBarelyConfiguredRequestedCluster() {
        Cluster requestedCluster = new Cluster();
        requestedCluster.setRdsConfigs(new LinkedHashSet<>());
        requestedCluster.setBlueprint(blueprint);
        return requestedCluster;
    }

    private Set<RDSConfig> createRdsConfigs(ResourceStatus... statuses) {
        Set<RDSConfig> configs = new LinkedHashSet<>(statuses.length);
        long id = 0;
        for (ResourceStatus status : statuses) {
            RDSConfig config = new RDSConfig();
            config.setId(id++);
            config.setStatus(status);
            configs.add(config);
        }
        return configs;
    }

    private List<BlueprintParameter> createBlueprintParameters() {
        List<BlueprintParameter> parameters = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            BlueprintParameter parameter = new BlueprintParameter();
            parameter.setName("name");
            parameter.setReferenceConfiguration("configuration reference content");
            parameter.setDescription("description");
            parameters.add(parameter);
        }
        return parameters;
    }

}