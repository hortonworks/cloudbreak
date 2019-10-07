package com.sequenceiq.cloudbreak.cm;

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
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiUser2;
import com.cloudera.api.swagger.model.ApiUser2List;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@RunWith(MockitoJUnitRunner.class)
public class ClouderaManagerSecurityServiceTest {

    private static final int GATEWAY_PORT = 8080;

    private static final boolean LDAP_ENABLED = true;

    private static final boolean LDAP_DISABLED = false;

    private ClouderaManagerClientFactory clouderaManagerClientFactory = mock(ClouderaManagerClientFactory.class);

    private ClouderaManagerSecurityConfigProvider securityConfigProvider = mock(ClouderaManagerSecurityConfigProvider.class);

    private ClouderaManagerKerberosService kerberosService = mock(ClouderaManagerKerberosService.class);

    private ClouderaManagerLdapService ldapService = mock(ClouderaManagerLdapService.class);

    @InjectMocks
    private ClouderaManagerSecurityService underTest;

    private Stack stack;

    private HttpClientConfig clientConfig = new HttpClientConfig("localhost");

    public void createUnderTest(String userName) {
        stack = createStack(userName);
        underTest = new ClouderaManagerSecurityService(stack, clientConfig);
        ReflectionTestUtils.setField(underTest, "clouderaManagerClientFactory", clouderaManagerClientFactory);
        ReflectionTestUtils.setField(underTest, "securityConfigProvider", securityConfigProvider);
        ReflectionTestUtils.setField(underTest, "kerberosService", kerberosService);
        ReflectionTestUtils.setField(underTest, "ldapService", ldapService);
    }

    @Test
    public void testChangeOriginalCredentialsAndCreateCloudbreakUserWhenLdapIsConfiguredAndAdminUserIsProvided()
            throws CloudbreakException, ApiException, ClouderaManagerClientInitException {
        createUnderTest("admin");
        UsersResourceApi usersResourceApi = mock(UsersResourceApi.class);
        ApiUser2List oldUserList = createApiUser2List();

        when(clouderaManagerClientFactory.getDefaultUsersResourceApi(GATEWAY_PORT, clientConfig)).thenReturn(usersResourceApi);
        when(usersResourceApi.readUsers2("SUMMARY")).thenReturn(oldUserList);

        underTest.changeOriginalCredentialsAndCreateCloudbreakUser(LDAP_ENABLED);

        verify(clouderaManagerClientFactory).getDefaultUsersResourceApi(GATEWAY_PORT, clientConfig);
        verify(usersResourceApi).readUsers2("SUMMARY");

        ArgumentCaptor<ApiUser2List> argumentCaptor = ArgumentCaptor.forClass(ApiUser2List.class);
        verify(usersResourceApi, times(2)).createUsers2(argumentCaptor.capture());
        List<ApiUser2List> createdUsers = argumentCaptor.getAllValues();
        Assert.assertEquals(stack.getCluster().getCloudbreakAmbariUser(), createdUsers.get(0).getItems().get(0).getName());
        Assert.assertEquals(stack.getCluster().getCloudbreakAmbariPassword(), createdUsers.get(0).getItems().get(0).getPassword());
        Assert.assertEquals(stack.getCluster().getDpAmbariUser(), createdUsers.get(1).getItems().get(0).getName());
        Assert.assertEquals(stack.getCluster().getDpAmbariPassword(), createdUsers.get(1).getItems().get(0).getPassword());

        verify(usersResourceApi).updateUser2(oldUserList.getItems().get(0).getName(), oldUserList.getItems().get(0));
        verifyNoMoreInteractions(clouderaManagerClientFactory);
    }

    @Test
    public void testChangeOriginalCredentialsAndCreateCloudbreakUserWhenLdapIsNotConfiguredAndTheGivenUserIsNotAdmin()
            throws CloudbreakException, ApiException, ClouderaManagerClientInitException {
        createUnderTest("ambariUser");
        UsersResourceApi usersResourceApi = mock(UsersResourceApi.class);
        UsersResourceApi newUsersResourceApi = mock(UsersResourceApi.class);
        ApiUser2List oldUserList = createApiUser2List();

        when(clouderaManagerClientFactory.getDefaultUsersResourceApi(GATEWAY_PORT, clientConfig)).thenReturn(usersResourceApi);
        when(usersResourceApi.readUsers2("SUMMARY")).thenReturn(oldUserList);
        when(clouderaManagerClientFactory.getUserResourceApi(GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword(), clientConfig)).thenReturn(newUsersResourceApi);

        underTest.changeOriginalCredentialsAndCreateCloudbreakUser(LDAP_DISABLED);

        verify(clouderaManagerClientFactory).getDefaultUsersResourceApi(GATEWAY_PORT, clientConfig);
        verify(usersResourceApi).readUsers2("SUMMARY");

        verify(clouderaManagerClientFactory).getUserResourceApi(GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword(), clientConfig);

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

        verifyNoMoreInteractions(clouderaManagerClientFactory);
    }

    @Test
    public void testChangeOriginalCredentialsAndCreateCloudbreakUserWhenLdapIsConfiguredAndTheGivenUserIsNotAdmin()
            throws CloudbreakException, ApiException, ClouderaManagerClientInitException {
        createUnderTest("ambariUser");
        UsersResourceApi usersResourceApi = mock(UsersResourceApi.class);
        UsersResourceApi newUsersResourceApi = mock(UsersResourceApi.class);
        ApiUser2List oldUserList = createApiUser2List();

        when(clouderaManagerClientFactory.getDefaultUsersResourceApi(GATEWAY_PORT, clientConfig)).thenReturn(usersResourceApi);
        when(usersResourceApi.readUsers2("SUMMARY")).thenReturn(oldUserList);
        when(clouderaManagerClientFactory.getUserResourceApi(GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword(), clientConfig)).thenReturn(newUsersResourceApi);

        underTest.changeOriginalCredentialsAndCreateCloudbreakUser(LDAP_ENABLED);

        verify(clouderaManagerClientFactory).getDefaultUsersResourceApi(GATEWAY_PORT, clientConfig);
        verify(usersResourceApi).readUsers2("SUMMARY");

        verify(clouderaManagerClientFactory, times(2)).getUserResourceApi(GATEWAY_PORT, stack.getCluster().getCloudbreakAmbariUser(),
                stack.getCluster().getCloudbreakAmbariPassword(), clientConfig);

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

        verify(newUsersResourceApi).deleteUser2("admin");
    }

    private Stack createStack(String userName) {
        Stack stack = new Stack();
        stack.setGatewayPort(GATEWAY_PORT);
        stack.setCluster(createCluster(userName));
        return stack;
    }

    private Cluster createCluster(String userName) {
        Cluster cluster = new Cluster();
        cluster.setCloudbreakAmbariUser("cloudbereak");
        cluster.setCloudbreakAmbariPassword("cloudbereak123");
        cluster.setDpAmbariUser("dp");
        cluster.setDpAmbariPassword("dp123");
        cluster.setUserName(userName);
        cluster.setPassword("admin123");
        return cluster;
    }

    private ApiUser2List createApiUser2List() {
        ApiUser2List apiUser2List = new ApiUser2List();
        ApiUser2 admin = new ApiUser2();
        admin.setName("admin");
        admin.setAuthRoles(Collections.singletonList(new ApiAuthRoleRef()));
        apiUser2List.setItems(List.of(admin));
        return apiUser2List;
    }

}