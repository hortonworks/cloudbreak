package com.sequenceiq.cloudbreak.cm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.UsersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiUser2;
import com.cloudera.api.swagger.model.ApiUser2List;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@RunWith(MockitoJUnitRunner.class)
public class
ClouderaManagerSecurityServiceTest {

    private static final int GATEWAY_PORT = 8080;

    private static final boolean LDAP_ENABLED = true;

    private static final boolean LDAP_DISABLED = false;

    private static final String ADMIN = "admin";

    private final ClouderaManagerApiFactory clouderaManagerApiFactory = mock(ClouderaManagerApiFactory.class);

    private final ClouderaManagerApiClientProvider clouderaManagerApiClientProvider = mock(ClouderaManagerApiClientProvider.class);

    private final ClouderaManagerSecurityConfigProvider securityConfigProvider = mock(ClouderaManagerSecurityConfigProvider.class);

    private final ClouderaManagerKerberosService kerberosService = mock(ClouderaManagerKerberosService.class);

    private final ClouderaManagerLdapService ldapService = mock(ClouderaManagerLdapService.class);

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
    }

    @Test
    public void testChangeOriginalCredentialsAndCreateCloudbreakUserWhenLdapIsConfiguredAndAdminUserIsProvided()
            throws CloudbreakException, ApiException, ClouderaManagerClientInitException {
        initTestInput(ADMIN);
        UsersResourceApi usersResourceApi = mock(UsersResourceApi.class);
        ApiUser2List oldUserList = createApiUser2List();

        when(clouderaManagerApiClientProvider.getDefaultClient(GATEWAY_PORT, clientConfig)).thenReturn(apiClient);
        when(clouderaManagerApiFactory.getUserResourceApi(any())).thenReturn(usersResourceApi);
        when(usersResourceApi.readUsers2("SUMMARY")).thenReturn(oldUserList);

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
        ApiClient newApiClient = mock(ApiClient.class);
        ApiUser2List oldUserList = createApiUser2List();

        when(clouderaManagerApiClientProvider.getDefaultClient(GATEWAY_PORT, clientConfig)).thenReturn(apiClient);
        when(clouderaManagerApiFactory.getUserResourceApi(apiClient)).thenReturn(usersResourceApi);

        when(usersResourceApi.readUsers2("SUMMARY")).thenReturn(oldUserList);

        when(clouderaManagerApiClientProvider.getClouderaManagerClient(clientConfig, GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword())).thenReturn(newApiClient);
        when(clouderaManagerApiFactory.getUserResourceApi(newApiClient)).thenReturn(newUsersResourceApi);

        underTest.changeOriginalCredentialsAndCreateCloudbreakUser(LDAP_DISABLED);

        verify(clouderaManagerApiClientProvider).getDefaultClient(GATEWAY_PORT, clientConfig);
        verify(usersResourceApi).readUsers2("SUMMARY");

        verify(clouderaManagerApiClientProvider).getClouderaManagerClient(clientConfig, GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword());

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
    public void testChangeOriginalCredentialsAndCreateCloudbreakUserWhenLdapIsConfiguredAndTheGivenUserIsNotAdmin()
            throws CloudbreakException, ApiException, ClouderaManagerClientInitException {
        initTestInput("ambariUser");
        UsersResourceApi usersResourceApi = mock(UsersResourceApi.class);
        UsersResourceApi newUsersResourceApi = mock(UsersResourceApi.class);
        ApiClient newApiClient = mock(ApiClient.class);
        ApiUser2List oldUserList = createApiUser2List();

        when(clouderaManagerApiClientProvider.getDefaultClient(GATEWAY_PORT, clientConfig)).thenReturn(apiClient);
        when(clouderaManagerApiFactory.getUserResourceApi(apiClient)).thenReturn(usersResourceApi);

        when(usersResourceApi.readUsers2("SUMMARY")).thenReturn(oldUserList);

        when(clouderaManagerApiClientProvider.getClouderaManagerClient(clientConfig, GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword())).thenReturn(newApiClient);
        when(clouderaManagerApiFactory.getUserResourceApi(newApiClient)).thenReturn(newUsersResourceApi);

        underTest.changeOriginalCredentialsAndCreateCloudbreakUser(LDAP_ENABLED);

        verify(clouderaManagerApiClientProvider).getDefaultClient(GATEWAY_PORT, clientConfig);
        verify(usersResourceApi).readUsers2("SUMMARY");
        verify(clouderaManagerApiClientProvider, times(2)).getClouderaManagerClient(clientConfig, GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword());

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

}