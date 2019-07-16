package com.sequenceiq.cloudbreak.cm;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.UsersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiUser2;
import com.cloudera.api.swagger.model.ApiUser2List;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.util.UsersResourceApiProvider;
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
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Inject
    private ClouderaManagerSecurityConfigProvider securityConfigProvider;

    @Inject
    private ClouderaManagerKerberosService kerberosService;

    @Inject
    private ClouderaManagerLdapService ldapService;

    @Inject
    private UsersResourceApiProvider usersResourceApiProvider;

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
        UsersResourceApi usersResourceApi = usersResourceApiProvider.getResourceApi(stack.getGatewayPort(), user, password, clientConfig);
        try {
            ApiUser2List oldUserList = usersResourceApi.readUsers2("SUMMARY");
            Optional<ApiUser2> oldAdminUser = oldUserList.getItems().stream()
                    .filter(apiUser2 -> apiUser2.getName().equals(stack.getCluster().getUserName()))
                    .findFirst();
            if (oldAdminUser.isPresent()) {
                createNewUser(usersResourceApi, oldAdminUser.get().getAuthRoles(), newUserName, newPassword);
                usersResourceApi.deleteUser2(oldAdminUser.get().getName());
            } else {
                throw new CloudbreakException("Can't find original admin user");
            }
        } catch (ApiException e) {
            LOGGER.info("Can't replace original admin user due to: ", e);
            throw new CloudbreakException("Can't replace original admin user due to: " + e.getMessage());
        }
    }

    @Override
    public void updateUserNamePassword(String newPassword) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        String cmUser = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        UsersResourceApi usersResourceApi = usersResourceApiProvider.getResourceApi(stack.getGatewayPort(), cmUser, password, clientConfig);
        try {
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
        } catch (ApiException e) {
            LOGGER.info("Can't replace admin password due to: ", e);
            throw new CloudbreakException("Can't replace admin password due to: " + e.getMessage());
        }
    }

    @Override
    public void prepareSecurity() {

    }

    @Override
    public void disableSecurity() {
        Cluster cluster = stack.getCluster();
        String user = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        ApiClient client = clouderaManagerClientFactory.getClient(stack.getGatewayPort(), user, password, clientConfig);
        kerberosService.deleteCredentials(client, clientConfig, stack);
    }

    @Override
    public void changeOriginalCredentialsAndCreateCloudbreakUser(boolean ldapConfigured) throws CloudbreakException {
        LOGGER.debug("change original admin user and create cloudbreak user");
        UsersResourceApi usersResourceApi = usersResourceApiProvider.getDefaultUsersResourceApi(stack.getGatewayPort(), clientConfig);
        try {
            Optional<ApiUser2> oldAdminUser = getOldAdminUser(usersResourceApi);
            if (oldAdminUser.isPresent()) {
                Cluster cluster = stack.getCluster();
                createNewUser(usersResourceApi, oldAdminUser.get().getAuthRoles(), cluster.getCloudbreakAmbariUser(), cluster.getCloudbreakAmbariPassword());
                createNewUser(usersResourceApi, oldAdminUser.get().getAuthRoles(), cluster.getDpAmbariUser(), cluster.getDpAmbariPassword());
                if (ADMIN_USER.equals(cluster.getUserName())) {
                    ApiUser2 oldAdmin = oldAdminUser.get();
                    oldAdmin.setPassword(cluster.getPassword());
                    usersResourceApi.updateUser2(oldAdminUser.get().getName(), oldAdmin);
                } else {
                    if (cluster.getUserName() != null) {
                        String user = cluster.getCloudbreakAmbariUser();
                        String password = cluster.getCloudbreakAmbariPassword();
                        UsersResourceApi newUsersResourceApi = usersResourceApiProvider.getResourceApi(stack.getGatewayPort(), user, password, clientConfig);
                        createNewUser(newUsersResourceApi, oldAdminUser.get().getAuthRoles(), cluster.getUserName(), cluster.getPassword());
                    }
                }
                removeDefaultAdminUser(ldapConfigured, Optional.ofNullable(cluster.getUserName()));
            } else {
                throw new CloudbreakException("Can't find original admin user");
            }
        } catch (ApiException e) {
            LOGGER.info("Can't replace original admin user due to: ", e);
            throw new CloudbreakException("Can't replace original admin user due to: " + e.getMessage());
        }
    }

    private void removeDefaultAdminUser(boolean ldapConfigured, Optional<String> userName) {
        if (ldapConfigured && isUserIsNullOrNotAdmin(userName)) {
            try {
                String user = stack.getCluster().getCloudbreakAmbariUser();
                String password = stack.getCluster().getCloudbreakAmbariPassword();
                UsersResourceApi usersResourceApi = usersResourceApiProvider.getResourceApi(stack.getGatewayPort(), user, password, clientConfig);
                usersResourceApi.deleteUser2(ADMIN_USER);
            } catch (ApiException e) {
                LOGGER.info("Can't remove default admin user due to: ", e);
            }
        }
    }

    private boolean isUserIsNullOrNotAdmin(Optional<String> userName) {
        return userName.isEmpty() || !ADMIN_USER.equals(userName.get());
    }

    private Optional<ApiUser2> getOldAdminUser(UsersResourceApi usersResourceApi) throws ApiException {
        ApiUser2List oldUserList = usersResourceApi.readUsers2("SUMMARY");
        return oldUserList.getItems().stream()
                .filter(apiUser2 -> ADMIN_USER.equals(apiUser2.getName()))
                .findFirst();
    }

    @Override
    public void setupLdapAndSSO(String primaryGatewayPublicAddress, LdapView ldapConfig) throws CloudbreakException {
        try {
            ldapService.setupLdap(stack, stack.getCluster(), clientConfig, ldapConfig);
        } catch (ApiException apiException) {
            throw new CloudbreakException(apiException);
        }
    }

    @Override
    public boolean isLdapAndSSOReady(AmbariRepo ambariRepo) {
        return false;
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

    private void createNewUser(UsersResourceApi usersResourceApi, List<ApiAuthRoleRef> authRoles, String userName, String password) throws ApiException {
        ApiUser2List apiUser2List = new ApiUser2List();
        ApiUser2 newUser = new ApiUser2();
        newUser.setName(userName);
        newUser.setPassword(password);
        newUser.setAuthRoles(authRoles);
        apiUser2List.addItemsItem(newUser);
        usersResourceApi.createUsers2(apiUser2List);
    }
}
