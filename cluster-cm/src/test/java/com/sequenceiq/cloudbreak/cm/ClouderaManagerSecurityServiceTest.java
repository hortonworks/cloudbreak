package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.BatchResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.ToolsResourceApi;
import com.cloudera.api.swagger.UsersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiBatchRequest;
import com.cloudera.api.swagger.model.ApiBatchResponse;
import com.cloudera.api.swagger.model.ApiBatchResponseElement;
import com.cloudera.api.swagger.model.ApiClusterRef;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiEcho;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiUser2;
import com.cloudera.api.swagger.model.ApiUser2List;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@RunWith(MockitoJUnitRunner.class)
public class ClouderaManagerSecurityServiceTest {

    private static final int GATEWAY_PORT = 8080;

    private static final boolean LDAP_ENABLED = true;

    private static final boolean LDAP_DISABLED = false;

    private static final String ADMIN = "admin";

    private final ClouderaManagerApiFactory clouderaManagerApiFactory = mock(ClouderaManagerApiFactory.class);

    private final ClouderaManagerApiClientProvider clouderaManagerApiClientProvider = mock(ClouderaManagerApiClientProvider.class);

    private final ClouderaManagerSecurityConfigProvider securityConfigProvider = mock(ClouderaManagerSecurityConfigProvider.class);

    private final ClouderaManagerKerberosService kerberosService = mock(ClouderaManagerKerberosService.class);

    private final ClouderaManagerLdapService ldapService = mock(ClouderaManagerLdapService.class);

    private final ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider = mock(ClouderaManagerPollingServiceProvider.class);

    private final ApiClient apiClient = mock(ApiClient.class);

    @InjectMocks
    private ClouderaManagerSecurityService underTest;

    private Stack stack;

    private final HttpClientConfig clientConfig = new HttpClientConfig("localhost");

    public void initTestInput(String userName) {
        stack = createStack(userName);
        underTest = new ClouderaManagerSecurityService(stack, clientConfig);
        ReflectionTestUtils.setField(underTest, "clouderaManagerApiFactory", clouderaManagerApiFactory);
        ReflectionTestUtils.setField(underTest, "clouderaManagerApiClientProvider", clouderaManagerApiClientProvider);
        ReflectionTestUtils.setField(underTest, "securityConfigProvider", securityConfigProvider);
        ReflectionTestUtils.setField(underTest, "kerberosService", kerberosService);
        ReflectionTestUtils.setField(underTest, "ldapService", ldapService);
        ReflectionTestUtils.setField(underTest, "clouderaManagerPollingServiceProvider", clouderaManagerPollingServiceProvider);
    }

    @Test
    public void testChangeOriginalCredentialsAndCreateCloudbreakUserWhenLdapIsConfiguredAndAdminUserIsProvided()
            throws CloudbreakException, ApiException, ClouderaManagerClientInitException {
        initTestInput(ADMIN);
        UsersResourceApi usersResourceApi = mock(UsersResourceApi.class);
        ToolsResourceApi toolsResourceApi = mock(ToolsResourceApi.class);
        ApiUser2List oldUserList = createApiUser2List();

        when(clouderaManagerApiClientProvider.getDefaultClient(GATEWAY_PORT, clientConfig, ClouderaManagerApiClientProvider.API_V_31)).thenReturn(apiClient);
        when(clouderaManagerApiFactory.getUserResourceApi(any())).thenReturn(usersResourceApi);
        when(usersResourceApi.readUsers2("SUMMARY")).thenReturn(oldUserList);
        when(clouderaManagerApiFactory.getToolsResourceApi(any())).thenReturn(toolsResourceApi);
        when(toolsResourceApi.echo("TEST")).thenReturn(new ApiEcho());

        underTest.changeOriginalCredentialsAndCreateCloudbreakUser(LDAP_ENABLED);

        verify(clouderaManagerApiFactory).getUserResourceApi(apiClient);
        verify(usersResourceApi).readUsers2("SUMMARY");

        ArgumentCaptor<ApiUser2List> argumentCaptor = ArgumentCaptor.forClass(ApiUser2List.class);
        verify(usersResourceApi, times(2)).createUsers2(argumentCaptor.capture());
        List<ApiUser2List> createdUsers = argumentCaptor.getAllValues();
        Assert.assertEquals(stack.getCluster().getCloudbreakAmbariUser(), createdUsers.get(0).getItems().get(0).getName());
        Assert.assertEquals(stack.getCluster().getCloudbreakAmbariPassword(), createdUsers.get(0).getItems().get(0).getPassword());
        Assert.assertEquals(stack.getCluster().getDpAmbariUser(), createdUsers.get(1).getItems().get(0).getName());
        Assert.assertEquals(stack.getCluster().getDpAmbariPassword(), createdUsers.get(1).getItems().get(0).getPassword());

        verify(usersResourceApi).updateUser2(oldUserList.getItems().get(0).getName(), oldUserList.getItems().get(0));
    }

    @Test
    public void testChangeOriginalCredentialsAndCreateCloudbreakUserWhenLdapIsNotConfiguredAndTheGivenUserIsNotAdmin()
            throws CloudbreakException, ApiException, ClouderaManagerClientInitException {
        initTestInput("ambariUser");
        UsersResourceApi usersResourceApi = mock(UsersResourceApi.class);
        UsersResourceApi newUsersResourceApi = mock(UsersResourceApi.class);
        ToolsResourceApi toolsResourceApi = mock(ToolsResourceApi.class);
        ApiClient newApiClient = mock(ApiClient.class);
        ApiUser2List oldUserList = createApiUser2List();

        when(clouderaManagerApiClientProvider.getDefaultClient(GATEWAY_PORT, clientConfig, ClouderaManagerApiClientProvider.API_V_31)).thenReturn(apiClient);
        when(clouderaManagerApiFactory.getUserResourceApi(apiClient)).thenReturn(usersResourceApi);

        when(usersResourceApi.readUsers2("SUMMARY")).thenReturn(oldUserList);

        when(clouderaManagerApiFactory.getToolsResourceApi(any())).thenReturn(toolsResourceApi);
        when(toolsResourceApi.echo("TEST")).thenReturn(new ApiEcho());

        when(clouderaManagerApiClientProvider.getClouderaManagerClient(clientConfig, GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword(), ClouderaManagerApiClientProvider.API_V_31)).thenReturn(newApiClient);
        when(clouderaManagerApiFactory.getUserResourceApi(newApiClient)).thenReturn(newUsersResourceApi);

        underTest.changeOriginalCredentialsAndCreateCloudbreakUser(LDAP_DISABLED);

        verify(clouderaManagerApiClientProvider).getDefaultClient(GATEWAY_PORT, clientConfig, ClouderaManagerApiClientProvider.API_V_31);
        verify(usersResourceApi).readUsers2("SUMMARY");

        verify(clouderaManagerApiClientProvider).getClouderaManagerClient(clientConfig, GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword(), ClouderaManagerApiClientProvider.API_V_31);

        ArgumentCaptor<ApiUser2List> createUserCaptor = ArgumentCaptor.forClass(ApiUser2List.class);
        verify(usersResourceApi, times(2)).createUsers2(createUserCaptor.capture());
        List<ApiUser2List> createdUsers = createUserCaptor.getAllValues();
        Assert.assertEquals(stack.getCluster().getCloudbreakAmbariUser(), createdUsers.get(0).getItems().get(0).getName());
        Assert.assertEquals(stack.getCluster().getCloudbreakAmbariPassword(), createdUsers.get(0).getItems().get(0).getPassword());
        Assert.assertEquals(stack.getCluster().getDpAmbariUser(), createdUsers.get(1).getItems().get(0).getName());
        Assert.assertEquals(stack.getCluster().getDpAmbariPassword(), createdUsers.get(1).getItems().get(0).getPassword());

        ArgumentCaptor<ApiUser2List> createNewUserCaptor = ArgumentCaptor.forClass(ApiUser2List.class);
        verify(newUsersResourceApi).createUsers2(createNewUserCaptor.capture());
        List<ApiUser2List> createdNewUser = createNewUserCaptor.getAllValues();
        Assert.assertEquals(stack.getCluster().getUserName(), createdNewUser.get(0).getItems().get(0).getName());
        Assert.assertEquals(stack.getCluster().getPassword(), createdNewUser.get(0).getItems().get(0).getPassword());

        verifyNoMoreInteractions(clouderaManagerApiClientProvider);
    }

    @Test
    public void testChangeOriginalCredentialsAndCreateCloudbreakUserWhenUsersAlreadyCreated()
            throws CloudbreakException, ApiException, ClouderaManagerClientInitException {
        initTestInput("ambariUser");
        UsersResourceApi usersResourceApi = mock(UsersResourceApi.class);
        UsersResourceApi newUsersResourceApi = mock(UsersResourceApi.class);
        ApiClient newApiClient = mock(ApiClient.class);

        Cluster cluster = stack.getCluster();

        setUpClientCreation(cluster);
        when(clouderaManagerApiFactory.getUserResourceApi(apiClient)).thenReturn(usersResourceApi);

        setUpUsersAlreadyCreated(usersResourceApi, cluster);
        setUpApiClientCredentialAlreadyChanged();

        when(clouderaManagerApiClientProvider.getClouderaManagerClient(clientConfig, GATEWAY_PORT, cluster.getCloudbreakAmbariUser(),
                cluster.getCloudbreakAmbariPassword(), ClouderaManagerApiClientProvider.API_V_31)).thenReturn(newApiClient);
        when(clouderaManagerApiFactory.getUserResourceApi(newApiClient)).thenReturn(newUsersResourceApi);

        underTest.changeOriginalCredentialsAndCreateCloudbreakUser(LDAP_DISABLED);

        verifyClientCreation(usersResourceApi, cluster);
        verify(usersResourceApi).readUsers2("SUMMARY");
        verifyNoUsersCreated(usersResourceApi, newUsersResourceApi);
        verifyNoMoreInteractions(clouderaManagerApiClientProvider);
    }

    private void setUpClientCreation(Cluster cluster) throws ClouderaManagerClientInitException {
        when(clouderaManagerApiClientProvider.getDefaultClient(GATEWAY_PORT, clientConfig, ClouderaManagerApiClientProvider.API_V_31)).thenReturn(apiClient);
        when(clouderaManagerApiClientProvider.getClient(GATEWAY_PORT, cluster.getCloudbreakAmbariUser(), cluster.getCloudbreakAmbariPassword(), clientConfig))
                .thenReturn(apiClient);
    }

    private void setUpApiClientCredentialAlreadyChanged() throws ApiException {
        ToolsResourceApi toolsResourceApi = mock(ToolsResourceApi.class);
        when(clouderaManagerApiFactory.getToolsResourceApi(any())).thenReturn(toolsResourceApi);
        when(toolsResourceApi.echo("TEST")).thenThrow(new ApiException(HttpStatus.UNAUTHORIZED.value(), "Bad credentials"));
    }

    private void setUpUsersAlreadyCreated(UsersResourceApi usersResourceApi, Cluster cluster) throws ApiException {
        ApiUser2List oldUserList = new ApiUser2List()
                .addItemsItem(new ApiUser2().name(ADMIN))
                .addItemsItem(new ApiUser2().name(cluster.getUserName()).password(cluster.getPassword()))
                .addItemsItem(new ApiUser2().name(cluster.getCloudbreakAmbariUser()).password(cluster.getCloudbreakAmbariPassword()))
                .addItemsItem(new ApiUser2().name(cluster.getDpAmbariUser()).password(cluster.getDpAmbariPassword()));
        when(usersResourceApi.readUsers2("SUMMARY")).thenReturn(oldUserList);
    }

    private void verifyClientCreation(UsersResourceApi usersResourceApi, Cluster cluster) throws ClouderaManagerClientInitException, ApiException {
        verify(clouderaManagerApiClientProvider).getDefaultClient(GATEWAY_PORT, clientConfig, ClouderaManagerApiClientProvider.API_V_31);
        verify(clouderaManagerApiClientProvider)
                .getClient(GATEWAY_PORT, cluster.getCloudbreakAmbariUser(), cluster.getCloudbreakAmbariPassword(), clientConfig);

        verify(clouderaManagerApiClientProvider).getClouderaManagerClient(clientConfig, GATEWAY_PORT, cluster.getCloudbreakAmbariUser(),
                cluster.getCloudbreakAmbariPassword(), ClouderaManagerApiClientProvider.API_V_31);
    }

    private void verifyNoUsersCreated(UsersResourceApi usersResourceApi, UsersResourceApi newUsersResourceApi) throws ApiException {
        verify(usersResourceApi, never()).createUsers2(any());
        verify(newUsersResourceApi, never()).createUsers2(any());
    }

    @Test
    public void testChangeOriginalCredentialsAndCreateCloudbreakUserWhenLdapIsConfiguredAndTheGivenUserIsNotAdmin()
            throws CloudbreakException, ApiException, ClouderaManagerClientInitException {
        initTestInput("ambariUser");
        UsersResourceApi usersResourceApi = mock(UsersResourceApi.class);
        UsersResourceApi newUsersResourceApi = mock(UsersResourceApi.class);
        ToolsResourceApi toolsResourceApi = mock(ToolsResourceApi.class);
        ApiClient newApiClient = mock(ApiClient.class);
        ApiUser2List oldUserList = createApiUser2List();

        when(clouderaManagerApiClientProvider.getDefaultClient(GATEWAY_PORT, clientConfig, ClouderaManagerApiClientProvider.API_V_31)).thenReturn(apiClient);
        when(clouderaManagerApiFactory.getUserResourceApi(apiClient)).thenReturn(usersResourceApi);

        when(usersResourceApi.readUsers2("SUMMARY")).thenReturn(oldUserList);

        when(clouderaManagerApiFactory.getToolsResourceApi(any())).thenReturn(toolsResourceApi);
        when(toolsResourceApi.echo("TEST")).thenReturn(new ApiEcho());

        when(clouderaManagerApiClientProvider.getClouderaManagerClient(clientConfig, GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword(), ClouderaManagerApiClientProvider.API_V_31)).thenReturn(newApiClient);
        when(clouderaManagerApiFactory.getUserResourceApi(newApiClient)).thenReturn(newUsersResourceApi);

        underTest.changeOriginalCredentialsAndCreateCloudbreakUser(LDAP_ENABLED);

        verify(clouderaManagerApiClientProvider).getDefaultClient(GATEWAY_PORT, clientConfig, ClouderaManagerApiClientProvider.API_V_31);
        verify(usersResourceApi).readUsers2("SUMMARY");
        verify(clouderaManagerApiClientProvider, times(2)).getClouderaManagerClient(clientConfig, GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword(), ClouderaManagerApiClientProvider.API_V_31);

        ArgumentCaptor<ApiUser2List> createUserCaptor = ArgumentCaptor.forClass(ApiUser2List.class);
        verify(usersResourceApi, times(2)).createUsers2(createUserCaptor.capture());
        List<ApiUser2List> createdUsers = createUserCaptor.getAllValues();
        Assert.assertEquals(stack.getCluster().getCloudbreakAmbariUser(), createdUsers.get(0).getItems().get(0).getName());
        Assert.assertEquals(stack.getCluster().getCloudbreakAmbariPassword(), createdUsers.get(0).getItems().get(0).getPassword());
        Assert.assertEquals(stack.getCluster().getDpAmbariUser(), createdUsers.get(1).getItems().get(0).getName());
        Assert.assertEquals(stack.getCluster().getDpAmbariPassword(), createdUsers.get(1).getItems().get(0).getPassword());

        ArgumentCaptor<ApiUser2List> createNewUserCaptor = ArgumentCaptor.forClass(ApiUser2List.class);
        verify(newUsersResourceApi).createUsers2(createNewUserCaptor.capture());
        List<ApiUser2List> createdNewUser = createNewUserCaptor.getAllValues();
        Assert.assertEquals(stack.getCluster().getUserName(), createdNewUser.get(0).getItems().get(0).getName());
        Assert.assertEquals(stack.getCluster().getPassword(), createdNewUser.get(0).getItems().get(0).getPassword());

        verify(newUsersResourceApi).deleteUser2(ADMIN);
    }

    @Test
    public void testRotateHostCertificates() throws Exception {
        // GIVEN
        initTestInput("user");
        when(clouderaManagerApiClientProvider.getClouderaManagerClient(clientConfig, GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword(), ClouderaManagerApiClientProvider.API_V_31)).thenReturn(apiClient);
        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        BatchResourceApi batchResourceApi = mock(BatchResourceApi.class);
        when(clouderaManagerApiFactory.getHostsResourceApi(apiClient)).thenReturn(hostsResourceApi);
        when(clouderaManagerApiFactory.getBatchResourceApi(apiClient)).thenReturn(batchResourceApi);
        ApiHostList hostList = createApiHostList();
        when(hostsResourceApi.readHosts(null, null, "SUMMARY")).thenReturn(hostList);
        ArgumentCaptor<ApiBatchRequest> batchRequestArgumentCaptor = ArgumentCaptor.forClass(ApiBatchRequest.class);
        when(batchResourceApi.execute(batchRequestArgumentCaptor.capture())).thenReturn(createApiBatchResponse(hostList, true));
        // WHEN
        underTest.rotateHostCertificates(null, null);
        // THEN no exception
        Assert.assertEquals(2, batchRequestArgumentCaptor.getValue().getItems().size());
    }

    @Test
    public void testRotateHostCertificatesWhenBatchExecuteFailed() throws Exception {
        // GIVEN
        initTestInput("user");
        when(clouderaManagerApiClientProvider.getClouderaManagerClient(clientConfig, GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword(), ClouderaManagerApiClientProvider.API_V_31)).thenReturn(apiClient);
        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        BatchResourceApi batchResourceApi = mock(BatchResourceApi.class);
        when(clouderaManagerApiFactory.getHostsResourceApi(apiClient)).thenReturn(hostsResourceApi);
        when(clouderaManagerApiFactory.getBatchResourceApi(apiClient)).thenReturn(batchResourceApi);
        ApiHostList hostList = createApiHostList();
        when(hostsResourceApi.readHosts(null, null, "SUMMARY")).thenReturn(hostList);
        ArgumentCaptor<ApiBatchRequest> batchRequestArgumentCaptor = ArgumentCaptor.forClass(ApiBatchRequest.class);
        when(batchResourceApi.execute(batchRequestArgumentCaptor.capture())).thenReturn(createApiBatchResponse(hostList, false));
        // WHEN
        assertThrows(ClouderaManagerOperationFailedException.class, () -> underTest.rotateHostCertificates(null, null));
        // THEN exception
    }

    @Test
    public void testRotateHostCertificatesWhenPollingCancelled() throws Exception {
        // GIVEN
        initTestInput("user");
        when(clouderaManagerApiClientProvider.getClouderaManagerClient(clientConfig, GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword(), ClouderaManagerApiClientProvider.API_V_31)).thenReturn(apiClient);
        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        BatchResourceApi batchResourceApi = mock(BatchResourceApi.class);
        when(clouderaManagerApiFactory.getHostsResourceApi(apiClient)).thenReturn(hostsResourceApi);
        when(clouderaManagerApiFactory.getBatchResourceApi(apiClient)).thenReturn(batchResourceApi);
        ApiHostList hostList = createApiHostList();
        when(hostsResourceApi.readHosts(null, null, "SUMMARY")).thenReturn(hostList);
        ArgumentCaptor<ApiBatchRequest> batchRequestArgumentCaptor = ArgumentCaptor.forClass(ApiBatchRequest.class);
        when(batchResourceApi.execute(batchRequestArgumentCaptor.capture())).thenReturn(createApiBatchResponse(hostList, true));
        when(clouderaManagerPollingServiceProvider.startPollingCommandList(eq(stack), eq(apiClient), any(List.class), eq("Rotate host certificates")))
                .thenReturn(PollingResult.EXIT);
        // WHEN
        assertThrows(CancellationException.class, () -> underTest.rotateHostCertificates(null, null));
        // THEN exception
    }

    @Test
    public void testRotateHostCertificatesWhenPollingTimedOut() throws Exception {
        // GIVEN
        initTestInput("user");
        when(clouderaManagerApiClientProvider.getClouderaManagerClient(clientConfig, GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword(), ClouderaManagerApiClientProvider.API_V_31)).thenReturn(apiClient);
        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        BatchResourceApi batchResourceApi = mock(BatchResourceApi.class);
        when(clouderaManagerApiFactory.getHostsResourceApi(apiClient)).thenReturn(hostsResourceApi);
        when(clouderaManagerApiFactory.getBatchResourceApi(apiClient)).thenReturn(batchResourceApi);
        ApiHostList hostList = createApiHostList();
        when(hostsResourceApi.readHosts(null, null, "SUMMARY")).thenReturn(hostList);
        ArgumentCaptor<ApiBatchRequest> batchRequestArgumentCaptor = ArgumentCaptor.forClass(ApiBatchRequest.class);
        when(batchResourceApi.execute(batchRequestArgumentCaptor.capture())).thenReturn(createApiBatchResponse(hostList, true));
        when(clouderaManagerPollingServiceProvider.startPollingCommandList(eq(stack), eq(apiClient), any(List.class), eq("Rotate host certificates")))
                .thenReturn(PollingResult.TIMEOUT);
        // WHEN
        assertThrows(ClouderaManagerOperationFailedException.class, () -> underTest.rotateHostCertificates(null, null));
        // THEN exception
    }

    @Test
    public void testRotateHostCertificatesWhenCMApiCallFailed() throws Exception {
        // GIVEN
        initTestInput("user");
        when(clouderaManagerApiClientProvider.getClouderaManagerClient(clientConfig, GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword(), ClouderaManagerApiClientProvider.API_V_31)).thenReturn(apiClient);
        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);
        BatchResourceApi batchResourceApi = mock(BatchResourceApi.class);
        when(clouderaManagerApiFactory.getHostsResourceApi(apiClient)).thenReturn(hostsResourceApi);
        when(clouderaManagerApiFactory.getBatchResourceApi(apiClient)).thenReturn(batchResourceApi);
        when(hostsResourceApi.readHosts(null, null, "SUMMARY")).thenThrow(new ApiException());
        // WHEN
        assertThrows(CloudbreakException.class, () -> underTest.rotateHostCertificates(null, null));
        // THEN exception
    }

    private Stack createStack(String userName) {
        Stack stack = new Stack();
        stack.setGatewayPort(GATEWAY_PORT);
        stack.setCluster(createCluster(userName));
        return stack;
    }

    private Cluster createCluster(String userName) {
        Cluster cluster = new Cluster();
        cluster.setCloudbreakUser("cloudbereak");
        cluster.setCloudbreakPassword("cloudbereak123");
        cluster.setDpUser("dp");
        cluster.setDpPassword("dp123");
        cluster.setUserName(userName);
        cluster.setPassword("admin123");
        return cluster;
    }

    private ApiUser2List createApiUser2List() {
        ApiUser2List apiUser2List = new ApiUser2List();
        ApiUser2 admin = new ApiUser2();
        admin.setName(ADMIN);
        admin.setAuthRoles(Collections.singletonList(new ApiAuthRoleRef()));
        apiUser2List.setItems(List.of(admin));
        return apiUser2List;
    }

    private ApiHostList createApiHostList() {
        ApiHostList apiHostList = new ApiHostList();
        ApiClusterRef clusterRef = new ApiClusterRef();
        ApiHost apiHost1 = new ApiHost().hostId("1").clusterRef(clusterRef);
        ApiHost apiHost2 = new ApiHost().hostId("2").clusterRef(clusterRef);
        apiHostList.items(List.of(apiHost1, apiHost2));
        return apiHostList;
    }

    private ApiBatchResponse createApiBatchResponse(ApiHostList hostList, boolean success) {
        List<ApiBatchResponseElement> responseElements = hostList.getItems().stream().map(req -> {
            ApiCommand apiCommand = new ApiCommand().active(Boolean.FALSE).id(BigDecimal.ONE).hostRef(new ApiHostRef().hostId(req.getHostId()));
            return new ApiBatchResponseElement().response(JsonUtil.writeValueAsStringSilent(apiCommand));
        }).collect(Collectors.toList());
        return new ApiBatchResponse().success(success).items(responseElements);
    }
}
