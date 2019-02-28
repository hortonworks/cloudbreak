package com.sequenceiq.cloudbreak.cm;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;

@Service
@Scope("prototype")
public class ClouderaManagerSecurityService implements ClusterSecurityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerSecurityService.class);

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Inject
    private ClouderaManagerSecurityConfigProvider securityConfigProvider;

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    private ApiClient client;

    public ClouderaManagerSecurityService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() {
        client = clouderaManagerClientFactory.getDefaultClient(stack, clientConfig);
    }

    @Override
    public void replaceUserNamePassword(String newUserName, String newPassword) throws CloudbreakException {
        UsersResourceApi usersResourceApi = new UsersResourceApi(client);
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
            throw new CloudbreakException("Can't replace original admin user");
        }
    }

    @Override
    public void updateUserNamePassword(String newPassword) throws CloudbreakException {
        UsersResourceApi usersResourceApi = new UsersResourceApi(client);
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
            throw new CloudbreakException("Can't replace admin password");
        }
    }

    @Override
    public void prepareSecurity() {

    }

    @Override
    public void disableSecurity() {

    }

    @Override
    public void changeOriginalCredentialsAndCreateCloudbreakUser() throws CloudbreakException {
        LOGGER.debug("change original admin user and create cloudbreak user");
        UsersResourceApi usersResourceApi = new UsersResourceApi(client);
        try {
            ApiUser2List oldUserList = usersResourceApi.readUsers2("SUMMARY");
            Optional<ApiUser2> oldAdminUser = oldUserList.getItems().stream()
                    .filter(apiUser2 -> apiUser2.getName().equals("admin"))
                    .findFirst();
            if (oldAdminUser.isPresent()) {
                Cluster cluster = stack.getCluster();
                createNewUser(usersResourceApi, oldAdminUser.get().getAuthRoles(), cluster.getCloudbreakAmbariUser(), cluster.getCloudbreakAmbariPassword());
                if ("admin".equals(cluster.getUserName())) {
                    ApiUser2 oldAdmin = oldAdminUser.get();
                    oldAdmin.setPassword(cluster.getPassword());
                    usersResourceApi.updateUser2(oldAdminUser.get().getName(), oldAdmin);
                } else {
                    createNewUser(usersResourceApi, oldAdminUser.get().getAuthRoles(), cluster.getUserName(), cluster.getPassword());
                    ApiClient newClient = clouderaManagerClientFactory.getClient(stack, cluster, clientConfig);
                    UsersResourceApi newUsersResourceApi = new UsersResourceApi(newClient);
                    newUsersResourceApi.deleteUser2(oldAdminUser.get().getName());
                }
            } else {
                throw new CloudbreakException("Can't find original admin user");
            }
        } catch (ApiException e) {
            throw new CloudbreakException("Can't replace original admin user");
        }
    }

    @Override
    public void setupLdapAndSSO(AmbariRepo ambariRepo, String primaryGatewayPublicAddress) {

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
