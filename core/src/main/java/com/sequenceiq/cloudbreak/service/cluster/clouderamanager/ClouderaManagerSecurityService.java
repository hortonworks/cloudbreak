package com.sequenceiq.cloudbreak.service.cluster.clouderamanager;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.UsersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiUser2;
import com.cloudera.api.swagger.model.ApiUser2List;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.api.ClusterSecurityService;

@Service
public class ClouderaManagerSecurityService implements ClusterSecurityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerSecurityService.class);

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Override
    public void replaceUserNamePassword(Stack stack, String newUserName, String newPassword) throws CloudbreakException {
        ApiClient client = clouderaManagerClientFactory.getDefaultClient(stack);
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
    public void updateUserNamePassword(Stack stack, String newPassword) throws CloudbreakException {
        ApiClient client = clouderaManagerClientFactory.getDefaultClient(stack);
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
    public void prepareSecurity(Stack stack) {

    }

    @Override
    public void disableSecurity(Stack stack) {

    }

    @Override
    public void changeOriginalCredentialsAndCreateCloudbreakUser(Stack stack) throws CloudbreakException {
        LOGGER.debug("change original admin user and create cloudbreak user");
        ApiClient client = clouderaManagerClientFactory.getDefaultClient(stack);
        UsersResourceApi usersResourceApi = new UsersResourceApi(client);
        try {
            ApiUser2List oldUserList = usersResourceApi.readUsers2("SUMMARY");
            Optional<ApiUser2> oldAdminUser = oldUserList.getItems().stream()
                    .filter(apiUser2 -> "admin".equals(apiUser2.getName()))
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
                    ApiClient newClient = clouderaManagerClientFactory.getClient(stack, cluster);
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
