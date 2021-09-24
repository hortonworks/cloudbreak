package com.sequenceiq.cloudbreak.cm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.cloudera.api.swagger.BatchResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.ToolsResourceApi;
import com.cloudera.api.swagger.UsersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiBatchRequest;
import com.cloudera.api.swagger.model.ApiBatchRequestElement;
import com.cloudera.api.swagger.model.ApiBatchResponse;
import com.cloudera.api.swagger.model.ApiBatchResponseElement;
import com.cloudera.api.swagger.model.ApiClusterRef;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiEcho;
import com.cloudera.api.swagger.model.ApiGenerateHostCertsArguments;
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

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerSecurityServiceTest {

    private static final int GATEWAY_PORT = 8080;

    private static final boolean LDAP_ENABLED = true;

    private static final boolean LDAP_DISABLED = false;

    private static final String ADMIN = "admin";

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Mock
    private ClouderaManagerSecurityConfigProvider securityConfigProvider;

    @Mock
    private ClouderaManagerKerberosService kerberosService;

    @Mock
    private ClouderaManagerLdapService ldapService;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private ApiClient apiClient;

    @InjectMocks
    private ClouderaManagerSecurityService underTest;

    private Stack stack;

    private HttpClientConfig clientConfig;

    public void initTestInput(String userName) {
        stack = createStack(userName);
        clientConfig = new HttpClientConfig("localhost");
        underTest = new ClouderaManagerSecurityService(stack, clientConfig);
        MockitoAnnotations.openMocks(this);
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
        assertEquals(stack.getCluster().getCloudbreakAmbariUser(), createdUsers.get(0).getItems().get(0).getName());
        assertEquals(stack.getCluster().getCloudbreakAmbariPassword(), createdUsers.get(0).getItems().get(0).getPassword());
        assertEquals(stack.getCluster().getDpAmbariUser(), createdUsers.get(1).getItems().get(0).getName());
        assertEquals(stack.getCluster().getDpAmbariPassword(), createdUsers.get(1).getItems().get(0).getPassword());

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
        assertEquals(stack.getCluster().getCloudbreakAmbariUser(), createdUsers.get(0).getItems().get(0).getName());
        assertEquals(stack.getCluster().getCloudbreakAmbariPassword(), createdUsers.get(0).getItems().get(0).getPassword());
        assertEquals(stack.getCluster().getDpAmbariUser(), createdUsers.get(1).getItems().get(0).getName());
        assertEquals(stack.getCluster().getDpAmbariPassword(), createdUsers.get(1).getItems().get(0).getPassword());

        ArgumentCaptor<ApiUser2List> createNewUserCaptor = ArgumentCaptor.forClass(ApiUser2List.class);
        verify(newUsersResourceApi).createUsers2(createNewUserCaptor.capture());
        List<ApiUser2List> createdNewUser = createNewUserCaptor.getAllValues();
        assertEquals(stack.getCluster().getUserName(), createdNewUser.get(0).getItems().get(0).getName());
        assertEquals(stack.getCluster().getPassword(), createdNewUser.get(0).getItems().get(0).getPassword());

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

        verifyClientCreation(cluster);
        verify(usersResourceApi).readUsers2("SUMMARY");
        verifyNoUsersCreated(usersResourceApi, newUsersResourceApi);
        verifyNoMoreInteractions(clouderaManagerApiClientProvider);
    }

    private void setUpClientCreation(Cluster cluster) throws ClouderaManagerClientInitException {
        when(clouderaManagerApiClientProvider.getDefaultClient(GATEWAY_PORT, clientConfig, ClouderaManagerApiClientProvider.API_V_31)).thenReturn(apiClient);
        when(clouderaManagerApiClientProvider.getV40Client(GATEWAY_PORT, cluster.getCloudbreakAmbariUser(), cluster.getCloudbreakAmbariPassword(), clientConfig))
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

    private void verifyClientCreation(Cluster cluster) throws ClouderaManagerClientInitException {
        verify(clouderaManagerApiClientProvider).getDefaultClient(GATEWAY_PORT, clientConfig, ClouderaManagerApiClientProvider.API_V_31);
        verify(clouderaManagerApiClientProvider)
                .getV40Client(GATEWAY_PORT, cluster.getCloudbreakAmbariUser(), cluster.getCloudbreakAmbariPassword(), clientConfig);

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
        assertEquals(stack.getCluster().getCloudbreakAmbariUser(), createdUsers.get(0).getItems().get(0).getName());
        assertEquals(stack.getCluster().getCloudbreakAmbariPassword(), createdUsers.get(0).getItems().get(0).getPassword());
        assertEquals(stack.getCluster().getDpAmbariUser(), createdUsers.get(1).getItems().get(0).getName());
        assertEquals(stack.getCluster().getDpAmbariPassword(), createdUsers.get(1).getItems().get(0).getPassword());

        ArgumentCaptor<ApiUser2List> createNewUserCaptor = ArgumentCaptor.forClass(ApiUser2List.class);
        verify(newUsersResourceApi).createUsers2(createNewUserCaptor.capture());
        List<ApiUser2List> createdNewUser = createNewUserCaptor.getAllValues();
        assertEquals(stack.getCluster().getUserName(), createdNewUser.get(0).getItems().get(0).getName());
        assertEquals(stack.getCluster().getPassword(), createdNewUser.get(0).getItems().get(0).getPassword());

        verify(newUsersResourceApi).deleteUser2(ADMIN);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testRotateHostCertificatesWhenBatchExecuteSucceededDataProvider")
    public void testRotateHostCertificates(String testCaseName, String subAltName) throws Exception {
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
        underTest.rotateHostCertificates(null, null, subAltName);
        // THEN no exception
        verifyBatchRequest(batchRequestArgumentCaptor.getValue(), subAltName, "/api/v31/hosts/host-1/commands/generateHostCerts",
                "/api/v31/hosts/host%40company.com%202/commands/generateHostCerts");
    }

    private void verifyBatchRequest(ApiBatchRequest batchRequest, String subAltName, String... urlsExpected) {
        assertThat(batchRequest).isNotNull();
        List<ApiBatchRequestElement> batchRequestElements = batchRequest.getItems();
        assertThat(batchRequestElements).isNotNull();
        if (urlsExpected == null) {
            assertThat(batchRequestElements).isEmpty();
        } else {
            assertThat(batchRequestElements).hasSize(urlsExpected.length);
            for (int i = 0; i < urlsExpected.length; i++) {
                ApiBatchRequestElement batchRequestElement = batchRequestElements.get(i);
                assertThat(batchRequestElement).isNotNull();
                assertThat(batchRequestElement.getUrl()).isEqualTo(urlsExpected[i]);
                ApiGenerateHostCertsArguments apiGenerateHostCertsArguments = (ApiGenerateHostCertsArguments) batchRequestElement.getBody();
                if (subAltName == null) {
                    assertNull(apiGenerateHostCertsArguments.getSubjectAltName());
                } else {
                    assertThat(apiGenerateHostCertsArguments.getSubjectAltName().get(0)).isEqualTo(subAltName);
                }
            }
        }
    }

    static Object[][] testRotateHostCertificatesWhenBatchExecuteSucceededDataProvider() {
        return new Object[][]{
                // testCaseName subAltName
                {"subAltName=null", null},
                {"subAltName!=null", "DNS:gateway.company.com"},
        };
    }

    static Object[][] testRotateHostCertificatesWhenBatchExecuteFailedDataProvider() {
        return new Object[][]{
                // testCaseName batchResponseFactory
                {"response=null", (Function<ApiHostList, ApiBatchResponse>) hostList -> null},
                {"success=null", (Function<ApiHostList, ApiBatchResponse>) hostList -> createApiBatchResponse(hostList, false).success(null)},
                {"items=null", (Function<ApiHostList, ApiBatchResponse>) hostList -> new ApiBatchResponse().success(true).items(null)},
                {"success=false", (Function<ApiHostList, ApiBatchResponse>) hostList -> createApiBatchResponse(hostList, false)},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testRotateHostCertificatesWhenBatchExecuteFailedDataProvider")
    public void testRotateHostCertificatesWhenBatchExecuteFailed(String testCaseName, Function<ApiHostList, ApiBatchResponse> batchResponseFactory)
            throws Exception {
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
        when(batchResourceApi.execute(batchRequestArgumentCaptor.capture())).thenReturn(batchResponseFactory.apply(hostList));
        // WHEN
        ClouderaManagerOperationFailedException exception = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.rotateHostCertificates(null, null, null));
        // THEN exception
        assertThat(exception).hasMessageStartingWith("Host certificates rotation batch operation failed: ");
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
        CancellationException exception = assertThrows(CancellationException.class, () -> underTest.rotateHostCertificates(null, null, null));
        // THEN exception
        assertThat(exception).hasMessage("Cluster was terminated during rotation of host certificates");
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
        ClouderaManagerOperationFailedException exception = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.rotateHostCertificates(null, null, null));
        // THEN exception
        assertThat(exception).hasMessage("Timeout while Cloudera Manager rotates the host certificates.");
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
        when(hostsResourceApi.readHosts(null, null, "SUMMARY")).thenThrow(new ApiException("Serious problem"));
        // WHEN
        CloudbreakException exception = assertThrows(CloudbreakException.class, () -> underTest.rotateHostCertificates(null, null, null));
        // THEN exception
        assertThat(exception).hasMessage("Can't rotate the host certificates due to: Serious problem");
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
        ApiHost apiHost1 = new ApiHost().hostId("host-1").clusterRef(clusterRef);
        ApiHost apiHost2 = new ApiHost().hostId("host@company.com 2").clusterRef(clusterRef);
        apiHostList.items(List.of(apiHost1, apiHost2));
        return apiHostList;
    }

    private static ApiBatchResponse createApiBatchResponse(ApiHostList hostList, boolean success) {
        List<ApiBatchResponseElement> responseElements = hostList.getItems().stream().map(req -> {
            ApiCommand apiCommand = new ApiCommand().active(Boolean.FALSE).id(BigDecimal.ONE).hostRef(new ApiHostRef().hostId(req.getHostId()));
            return new ApiBatchResponseElement().response(JsonUtil.writeValueAsStringSilent(apiCommand));
        }).collect(Collectors.toList());
        return new ApiBatchResponse().success(success).items(responseElements);
    }

}
