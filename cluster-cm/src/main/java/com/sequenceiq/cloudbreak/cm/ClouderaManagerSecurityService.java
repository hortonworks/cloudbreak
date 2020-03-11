package com.sequenceiq.cloudbreak.cm;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ToolsResourceApi;
import com.cloudera.api.swagger.UsersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiUser2;
import com.cloudera.api.swagger.model.ApiUser2List;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
@Scope("prototype")
public class ClouderaManagerSecurityService implements ClusterSecurityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerSecurityService.class);

    private static final String ADMIN_USER = "admin";

    @Inject
    private ClouderaManagerSecurityConfigProvider securityConfigProvider;

    @Inject
    private ClouderaManagerKerberosService kerberosService;

    @Inject
    private ClouderaManagerLdapService ldapService;

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    public ClouderaManagerSecurityService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @Override
    public void replaceUserNamePassword(String newUserName, String newPassword) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        String user = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        try {
            ApiClient client = getClient(stack.getGatewayPort(), user, password, clientConfig);
            UsersResourceApi usersResourceApi = clouderaManagerApiFactory.getUserResourceApi(client);
            ApiUser2List oldUserList = usersResourceApi.readUsers2("SUMMARY");
            Optional<ApiUser2> oldAdminUser = oldUserList.getItems().stream()
                    .filter(apiUser2 -> apiUser2.getName().equals(stack.getCluster().getUserName()))
                    .findFirst();
            if (oldAdminUser.isPresent()) {
                createNewUser(usersResourceApi, oldAdminUser.get().getAuthRoles(), newUserName, newPassword, oldUserList);
                usersResourceApi.deleteUser2(oldAdminUser.get().getName());
            } else {
                throw new CloudbreakException("Can't find original admin user");
            }
        } catch (ApiException | ClouderaManagerClientInitException e) {
            LOGGER.info("Can't replace original admin user due to: ", e);
            throw new CloudbreakException("Can't replace original admin user due to: " + e.getMessage());
        }
    }

    @Override
    public void updateUserNamePassword(String newPassword) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        String cmUser = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        try {
            ApiClient client = getClient(stack.getGatewayPort(), cmUser, password, clientConfig);
            UsersResourceApi usersResourceApi = clouderaManagerApiFactory.getUserResourceApi(client);
            ApiUser2List oldUserList = usersResourceApi.readUsers2("SUMMARY");
            Optional<ApiUser2> oldAdminUser = oldUserList.getItems().stream()
                    .filter(apiUser2 -> apiUser2.getName().equals(stack.getCluster().getUserName()))
                    .findFirst();
            if (oldAdminUser.isPresent()) {
                ApiUser2 user = oldAdminUser.get();
                user.setPassword(newPassword);
                usersResourceApi.updateUser2(user.getName(), user);
            } else {
                throw new CloudbreakException("Can't find admin user");
            }
        } catch (ApiException | ClouderaManagerClientInitException e) {
            LOGGER.info("Can't replace admin password due to: ", e);
            throw new CloudbreakException("Can't replace admin password due to: " + e.getMessage());
        }
    }

    @Override
    public void prepareSecurity() {

    }

    @Override
    public void disableSecurity() {
        try {
            kerberosService.deleteCredentials(clientConfig, stack);
        } catch (Exception e) {
            LOGGER.warn("Couldn't cleanup kerberos. It's possible CM isn't started.", e);
        }
    }

    @Override
    public void changeOriginalCredentialsAndCreateCloudbreakUser(boolean ldapConfigured) throws CloudbreakException {
        LOGGER.debug("change original admin user and create cloudbreak user");
        try {
            ApiClient client = createApiClient();
            UsersResourceApi usersResourceApi = clouderaManagerApiFactory.getUserResourceApi(client);
            ApiUser2List userList = usersResourceApi.readUsers2("SUMMARY");

            ApiUser2 oldAdminUser = getOldAdminUser(userList).orElseThrow(() -> new CloudbreakException("Can't find original admin user"));
            Cluster cluster = stack.getCluster();
            createNewUser(usersResourceApi, oldAdminUser.getAuthRoles(), cluster.getCloudbreakAmbariUser(), cluster.getCloudbreakAmbariPassword(), userList);
            createNewUser(usersResourceApi, oldAdminUser.getAuthRoles(), cluster.getDpAmbariUser(), cluster.getDpAmbariPassword(), userList);
            if (ADMIN_USER.equals(cluster.getUserName())) {
                ApiUser2 oldAdmin = oldAdminUser;
                oldAdmin.setPassword(cluster.getPassword());
                usersResourceApi.updateUser2(oldAdminUser.getName(), oldAdmin);
            } else if (cluster.getUserName() != null) {
                createUserSuppliedCMUser(userList, oldAdminUser, cluster);
            }
            removeDefaultAdminUser(ldapConfigured, Optional.ofNullable(cluster.getUserName()));
        } catch (ApiException | ClusterClientInitException | ClouderaManagerClientInitException e) {
            LOGGER.info("Can't replace original admin user due to: ", e);
            throw new CloudbreakException("Can't replace original admin user due to: " + e.getMessage());
        }
    }

    private void createUserSuppliedCMUser(ApiUser2List userList, ApiUser2 oldAdminUser, Cluster cluster)
            throws ClouderaManagerClientInitException, ApiException {
        ApiClient client;
        String user = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        client = getClient(stack.getGatewayPort(), user, password, clientConfig);
        UsersResourceApi newUsersResourceApi = clouderaManagerApiFactory.getUserResourceApi(client);
        createNewUser(newUsersResourceApi, oldAdminUser.getAuthRoles(), cluster.getUserName(), cluster.getPassword(), userList);
    }

    private boolean checkUserExists(ApiUser2List userList, String username) {
        return userList.getItems().stream().map(ApiUser2::getName).anyMatch(username::equals);
    }

    private ApiClient createApiClient() throws ClusterClientInitException, ClouderaManagerClientInitException, CloudbreakException {
        ApiClient client = null;
        try {
            client = clouderaManagerApiClientProvider.getDefaultClient(stack.getGatewayPort(), clientConfig, ClouderaManagerApiClientProvider.API_V_31);
            ToolsResourceApi toolsResourceApi = clouderaManagerApiFactory.getToolsResourceApi(client);
            toolsResourceApi.echo("TEST");
            LOGGER.debug("Cloudera Manager already running, old admin user's password has not been changed yet.");
            return client;
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        } catch (ApiException e) {
            if (org.springframework.http.HttpStatus.UNAUTHORIZED.value() == e.getCode()) {
                return createClientWithNewPassword();
            }
            LOGGER.debug("Cloudera Manager is not running.", e);
            throw new CloudbreakException("Cloudera Manager is not running. " + e.getMessage());
        }
    }

    private ApiClient createClientWithNewPassword() throws ClouderaManagerClientInitException {
        LOGGER.debug("Cloudera Manager already running, old admin user's password has been changed.");
        Cluster cluster = stack.getCluster();
        String user = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        return clouderaManagerApiClientProvider.getClient(stack.getGatewayPort(), user, password, clientConfig);
    }

    private void removeDefaultAdminUser(boolean ldapConfigured, Optional<String> userName) {
        if (ldapConfigured && isUserIsNullOrNotAdmin(userName)) {
            try {
                String user = stack.getCluster().getCloudbreakAmbariUser();
                String password = stack.getCluster().getCloudbreakAmbariPassword();
                ApiClient client = getClient(stack.getGatewayPort(), user, password, clientConfig);
                UsersResourceApi usersResourceApi = clouderaManagerApiFactory.getUserResourceApi(client);
                usersResourceApi.deleteUser2(ADMIN_USER);
            } catch (ApiException | ClouderaManagerClientInitException e) {
                LOGGER.info("Can't remove default admin user due to: ", e);
            }
        }
    }

    private boolean isUserIsNullOrNotAdmin(Optional<String> userName) {
        return userName.filter(ADMIN_USER::equals).map(x -> Boolean.FALSE).orElse(Boolean.TRUE);
    }

    @Retryable
    private Optional<ApiUser2> getOldAdminUser(ApiUser2List userList) {
        return userList.getItems().stream()
                .filter(apiUser2 -> ADMIN_USER.equals(apiUser2.getName()))
                .findFirst();
    }

    @Override
    public void setupLdapAndSSO(String primaryGatewayPublicAddress, LdapView ldapConfig, VirtualGroupRequest virtualGroupRequest) throws CloudbreakException {
        try {
            ldapService.setupLdap(stack, stack.getCluster(), clientConfig, ldapConfig, virtualGroupRequest);
        } catch (ApiException | ClouderaManagerClientInitException e) {
            throw new CloudbreakException(e);
        }
    }

    @Override
    public String getCloudbreakClusterUserName() {
        return securityConfigProvider.getCloudbreakClusterUserName(stack.getCluster());
    }

    @Override
    public String getCloudbreakClusterPassword() {
        return securityConfigProvider.getCloudbreakClusterPassword(stack.getCluster());
    }

    @Override
    public String getDataplaneClusterUserName() {
        return securityConfigProvider.getDataplaneClusterUserName(stack.getCluster());
    }

    @Override
    public String getDataplaneClusterPassword() {
        return securityConfigProvider.getDataplaneClusterPassword(stack.getCluster());
    }

    @Override
    public String getClusterUserProvidedPassword() {
        return securityConfigProvider.getClusterUserProvidedPassword(stack.getCluster());
    }

    @Override
    public String getCertPath() {
        return securityConfigProvider.getCertPath();
    }

    @Override
    public String getKeystorePath() {
        return securityConfigProvider.getKeystorePath();
    }

    @Override
    public String getKeystorePassword() {
        return securityConfigProvider.getKeystorePassword();
    }

    @Override
    public String getMasterKey() {
        return securityConfigProvider.getMasterKey(stack.getCluster());
    }

    private void createNewUser(UsersResourceApi usersResourceApi, List<ApiAuthRoleRef> authRoles, String userName, String password, ApiUser2List userList)
            throws ApiException {
        if (checkUserExists(userList, userName)) {
            return;
        }
        ApiUser2List apiUser2List = new ApiUser2List();
        ApiUser2 newUser = new ApiUser2();
        newUser.setName(userName);
        newUser.setPassword(password);
        newUser.setAuthRoles(authRoles);
        apiUser2List.addItemsItem(newUser);
        usersResourceApi.createUsers2(apiUser2List);
    }

    private ApiClient getDefaultClient(Integer gatewayPort, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        return clouderaManagerApiClientProvider.getDefaultClient(gatewayPort, clientConfig, ClouderaManagerApiClientProvider.API_V_31);
    }

    public ApiClient getClient(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        if (StringUtils.isNoneBlank(user, password)) {
            return clouderaManagerApiClientProvider.getClouderaManagerClient(clientConfig,
                    gatewayPort, user, password, ClouderaManagerApiClientProvider.API_V_31);
        } else {
            return getDefaultClient(gatewayPort, clientConfig);
        }
    }
}
