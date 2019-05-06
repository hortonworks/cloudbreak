package com.sequenceiq.it.cloudbreak.newway.mock.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiEcho;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiUser2;
import com.cloudera.api.swagger.model.ApiUser2List;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.cloudbreak.newway.mock.AbstractModelMock;
import com.sequenceiq.it.cloudbreak.newway.mock.DefaultModel;
import com.sequenceiq.it.spark.DynamicRouteStack;
import com.sequenceiq.it.util.HostNameUtil;

import spark.Service;

public class ClouderaManagerMock extends AbstractModelMock {

    public static final String API_ROOT = "/api/v31";

    public static final String ECHO = API_ROOT + "/tools/echo";

    public static final String USERS = API_ROOT + "/users";

    public static final String USERS_USER = API_ROOT + "/users/:user";

    public static final String IMPORT_CLUSTERTEMPLATE = API_ROOT + "/cm/importClusterTemplate";

    public static final String COMMANDS_COMMAND = API_ROOT + "/commands/:commandId";

    public static final String COMMANDS_STOP = API_ROOT + "/clusters/:cluster/commands/stop";

    public static final String COMMANDS_START = API_ROOT + "/clusters/:cluster/commands/start";

    public static final String HOSTS = API_ROOT + "/hosts";

    public static final String BEGIN_FREE_TRIAL = API_ROOT + "/cm/trial/begin";

    private DynamicRouteStack dynamicRouteStack;

    public ClouderaManagerMock(Service sparkService, DefaultModel defaultModel) {
        super(sparkService, defaultModel);
        dynamicRouteStack = new DynamicRouteStack(sparkService, defaultModel);
    }

    public DynamicRouteStack getDynamicRouteStack() {
        return dynamicRouteStack;
    }

    public void addClouderaManagerMappings() {
        getEcho();
        getUsers();
        putUser();
        postUser();
        postImportClusterTemplate();
        getCommand();
        postStopCommand();
        getHosts();
        postStartCommand();
        postBeginTrial();
    }

    private void getEcho() {
        dynamicRouteStack.get(ECHO, (request, response) -> {
            String message = request.queryMap("message").value();
            message = message == null ? "Hello World!" : message;
            return new ApiEcho().message(message);
        });
    }

    private void getUsers() {
        dynamicRouteStack.get(USERS, (request, response) -> getUserList());
    }

    private void putUser() {
        dynamicRouteStack.put(USERS_USER, (request, response) -> new ApiUser2().name(request.params("user")));
    }

    private void postUser() {
        dynamicRouteStack.post(USERS, (request, response) -> getUserList());
    }

    private ApiUser2List getUserList() {
        ApiUser2List apiUser2List = new ApiUser2List();
        ApiAuthRoleRef authRoleRef = new ApiAuthRoleRef().displayName("Full Administrator").uuid(UUID.randomUUID().toString());
        apiUser2List.addItemsItem(new ApiUser2().name("admin").addAuthRolesItem(authRoleRef));
        apiUser2List.addItemsItem(new ApiUser2().name("cloudbreak").addAuthRolesItem(authRoleRef));
        return apiUser2List;
    }

    private void postImportClusterTemplate() {
        dynamicRouteStack.post(IMPORT_CLUSTERTEMPLATE,
                (request, response) -> new ApiCommand().id(BigDecimal.ONE).name("Import ClusterTemplate").active(Boolean.TRUE));
    }

    private void getCommand() {
        dynamicRouteStack.get(COMMANDS_COMMAND,
                (request, response) -> new ApiCommand().id(new BigDecimal(request.params("commandId"))).active(Boolean.FALSE).success(Boolean.TRUE));
    }

    private void postStopCommand() {
        dynamicRouteStack.post(COMMANDS_STOP,
                (request, response) -> new ApiCommand().id(BigDecimal.ONE).active(Boolean.TRUE).name("Stop"));
    }

    private void postStartCommand() {
        dynamicRouteStack.post(COMMANDS_START,
                (request, response) -> new ApiCommand().id(BigDecimal.ONE).active(Boolean.TRUE).name("Start"));
    }

    private void postBeginTrial() {
        dynamicRouteStack.post(BEGIN_FREE_TRIAL, (request, response) -> null);
    }

    private void getHosts() {
        dynamicRouteStack.get(HOSTS, (request, response, model) -> {
            Map<String, CloudVmMetaDataStatus> instanceMap = model.getInstanceMap();
            ApiHostList apiHostList = new ApiHostList();
            for (Map.Entry<String, CloudVmMetaDataStatus> entry : instanceMap.entrySet()) {
                ApiHost apiHost = new ApiHost()
                        .hostId(entry.getValue().getCloudVmInstanceStatus().getCloudInstance().getInstanceId())
                        .hostname(HostNameUtil.generateHostNameByIp(entry.getValue().getMetaData().getPrivateIp()))
                        .ipAddress(entry.getValue().getMetaData().getPrivateIp());
                apiHostList.addItemsItem(apiHost);
            }
            return apiHostList;
        });
    }
}
