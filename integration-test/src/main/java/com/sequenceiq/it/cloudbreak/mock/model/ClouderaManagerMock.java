package com.sequenceiq.it.cloudbreak.mock.model;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.model.ApiAuthRoleMetadataList;
import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiEcho;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostTemplate;
import com.cloudera.api.swagger.model.ApiHostTemplateList;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.cloudera.api.swagger.model.ApiProductVersion;
import com.cloudera.api.swagger.model.ApiRemoteDataContext;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleConfigGroupRef;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.cloudera.api.swagger.model.ApiRoleTypeList;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.ApiServiceRef;
import com.cloudera.api.swagger.model.ApiServiceState;
import com.cloudera.api.swagger.model.ApiUser2;
import com.cloudera.api.swagger.model.ApiUser2List;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.cloudbreak.mock.AbstractModelMock;
import com.sequenceiq.it.cloudbreak.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
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

    public static final String COMMANDS = API_ROOT + "/clusters/:clusterName/commands";

    public static final String CLUSTER_DEPLOY_CLIENT_CONFIG = API_ROOT + "/clusters/:clusterName/commands/deployClientConfig";

    public static final String CM_REFRESH_PARCELREPOS = API_ROOT + "/cm/commands/refreshParcelRepos";

    public static final String CLUSTER_PARCELS = API_ROOT + "/clusters/:clusterName/parcels";

    public static final String CLUSTER_SERVICES = API_ROOT + "/clusters/:clusterName/services";

    public static final String CLUSTER_HOSTS = API_ROOT + "/clusters/:clusterName/hosts";

    public static final String CLUSTER_HOSTS_BY_HOSTID = API_ROOT + "/clusters/:clusterName/hosts/:hostId";

    public static final String CLUSTER_SERVICE_ROLES = API_ROOT + "/clusters/:clusterName/services/:serviceName/roles";

    public static final String CLUSTER_SERVICE_ROLES_BY_ROLE = API_ROOT + "/clusters/:clusterName/services/:serviceName/roles/:roleName";

    public static final String CLUSTER_HOSTTEMPLATES = API_ROOT + "/clusters/:clusterName/hostTemplates";

    public static final String CLUSTER_COMMANDS_REFRESH = API_ROOT + "/clusters/:clusterName/commands/refresh";

    public static final String CLUSTER_COMMANDS_RESTART = API_ROOT + "/clusters/:clusterName/commands/restart";

    public static final String HOSTS = API_ROOT + "/hosts";

    public static final String HOST_BY_ID = API_ROOT + "/hosts/:hostId";

    public static final String BEGIN_FREE_TRIAL = API_ROOT + "/cm/trial/begin";

    public static final String MANAGEMENT_SERVICE = API_ROOT + "/cm/service";

    public static final String START_MANAGEMENT_SERVICE = API_ROOT + "/cm/service/commands/start";

    public static final String ACTIVE_COMMANDS = API_ROOT + "/cm/service/commands";

    public static final String ROLE_TYPES = API_ROOT + "/cm/service/roleTypes";

    public static final String ROLES = API_ROOT + "/cm/service/roles";

    public static final String CONFIG = API_ROOT + "/cm/config";

    public static final String CM_RESTART = API_ROOT + "/cm/service/commands/restart";

    public static final String CM_HOST_DECOMMISSION = API_ROOT + "/cm/commands/hostsDecommission";

    public static final String CM_DELETE_CREDENTIALS = API_ROOT + "/cm/commands/deleteCredentials";

    public static final String LIST_COMMANDS = API_ROOT + "/cm/commands";

    public static final String READ_AUTH_ROLES = API_ROOT + "/authRoles/metadata";

    public static final String CDP_REMOTE_CONTEXT_BY_CLUSTER_CLUSTER_NAME = "/api/cdp/remoteContext/byCluster/:clusterName";

    public static final String CDP_REMOTE_CONTEXT = "/api/cdp/remoteContext";

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
        addManagementService();
        getManagementService();
        createRoles();
        listRoleTypes();
        listRoles();
        cmConfig();
        updateCmConfig();
        startManagementService();
        listCommands();
        listActiveCommands();
        readAuthRoles();
        getCdpRemoteContext();
        postCdpRemoteContext();

        getClusterServices();
        getClusterHosts();
        postCMRefreshParcelRepos();
        postClusterDeployClientConfig();
        getClusterParcels();
        postClusterCommandsRefresh();
        getClusterHostTemplates();
        getHostById();
        deleteHostById();
        postClouderaManagerHostDecommission();
        getClusterServiceRoles();
        deleteClusterServiceRole();
        deleteClusterHosts();
        postClouderaManagerDeleteCredentials();
        postClouderaManagerRestart();
        getCommands();
        postClusterCommandsRestart();
    }

    private void readAuthRoles() {
        dynamicRouteStack.get(READ_AUTH_ROLES, (request, response) -> new ApiAuthRoleMetadataList());
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
                (request, response, model) -> {
                    ApiClient client = new ApiClient();

                    Type type = ApiClusterTemplate.class;
                    ApiClusterTemplate template = client.getJSON().deserialize(request.body(), type);
                    model.setClouderaManagerProducts(template.getProducts());

                    return new ApiCommand().id(BigDecimal.ONE).name("Import ClusterTemplate").active(Boolean.TRUE);
                });
    }

    private void postClouderaManagerHostDecommission() {
        dynamicRouteStack.post(CM_HOST_DECOMMISSION,
                (request, response) -> getSuccessfulApiCommand());
    }

    private void postClouderaManagerDeleteCredentials() {
        dynamicRouteStack.post(CM_DELETE_CREDENTIALS,
                (request, response) -> getSuccessfulApiCommand());
    }

    private void postClouderaManagerRestart() {
        dynamicRouteStack.post(CM_RESTART,
                (request, response) -> getSuccessfulApiCommand());
    }

    private void getCommand() {
        dynamicRouteStack.get(COMMANDS_COMMAND,
                (request, response) -> new ApiCommand().id(new BigDecimal(request.params("commandId"))).active(Boolean.FALSE).success(Boolean.TRUE));
    }

    private void getCommands() {
        dynamicRouteStack.get(COMMANDS,
                (request, response) -> new ApiCommandList().items(List.of(new ApiCommand().name("something"))));
    }

    private void postStopCommand() {
        dynamicRouteStack.post(COMMANDS_STOP,
                (request, response) -> new ApiCommand().id(BigDecimal.ONE).active(Boolean.TRUE).name("Stop"));
    }

    private void postStartCommand() {
        dynamicRouteStack.post(COMMANDS_START,
                (request, response) -> new ApiCommand().id(BigDecimal.ONE).active(Boolean.TRUE).name("Start"));
    }

    private void getClusterServices() {
        dynamicRouteStack.get(CLUSTER_SERVICES,
                (request, response) -> new ApiServiceList().items(List.of(new ApiService().name("service1"))));
    }

    private void getClusterHosts() {
        dynamicRouteStack.get(CLUSTER_HOSTS,
                (request, response, model) ->
                        getHosts(model));
    }

    private void deleteClusterHosts() {
        dynamicRouteStack.delete(CLUSTER_HOSTS_BY_HOSTID,
                (request, response, model) ->
                        getHosts(model));
    }

    private void getClusterHostTemplates() {
        dynamicRouteStack.get(CLUSTER_HOSTTEMPLATES,
                (request, response, model) -> {
                    getHosts(model);

                    ApiHostTemplate hostTemplateWorker = new ApiHostTemplate()
                            .name("worker")
                            .roleConfigGroupRefs(
                                    List.of(new ApiRoleConfigGroupRef().roleConfigGroupName("WORKER")
                            ));
                    ApiHostTemplate hostTemplateCompute = new ApiHostTemplate()
                            .name("compute")
                            .roleConfigGroupRefs(
                                    List.of(new ApiRoleConfigGroupRef().roleConfigGroupName("DATANODE")
                                    ));
                    return new ApiHostTemplateList().items(List.of(hostTemplateWorker, hostTemplateCompute));
                });
    }

    private void getClusterServiceRoles() {
        dynamicRouteStack.get(CLUSTER_SERVICE_ROLES,
                (request, response) -> new ApiRoleList().items(List.of(
                        new ApiRole().name("role1").serviceRef(new ApiServiceRef().serviceName("service1"))
                )));
    }

    private void deleteClusterServiceRole() {
        dynamicRouteStack.delete(CLUSTER_SERVICE_ROLES_BY_ROLE,
                (request, response) -> new ApiRole().name("role1"));
    }

    private ApiCommand getSuccessfulApiCommand() {
        ApiCommand result = new ApiCommand();
        result.setId(BigDecimal.valueOf(1L));
        result.setActive(true);
        result.setSuccess(true);
        return result;
    }

    private void postCMRefreshParcelRepos() {
        dynamicRouteStack.post(CM_REFRESH_PARCELREPOS,
                (request, response) ->
                        getSuccessfulApiCommand());
    }

    private void getClusterParcels() {
        dynamicRouteStack.get(CLUSTER_PARCELS,
                (request, response, model) -> {
                    List<ApiProductVersion> products = model.getClouderaManagerProducts();

                    return new ApiParcelList().items(
                            products.stream().map(product -> new ApiParcel()
                                    .product(product.getProduct())
                                    .version(product.getVersion())
                                    .stage("ACTIVATED"))
                                    .collect(Collectors.toList())
                    );
                });
    }

    private void postClusterDeployClientConfig() {
        dynamicRouteStack.post(CLUSTER_DEPLOY_CLIENT_CONFIG,
                (request, response) ->
                        getSuccessfulApiCommand());
    }

    private void postClusterCommandsRefresh() {
        dynamicRouteStack.post(CLUSTER_COMMANDS_REFRESH,
                (request, response) ->
                        getSuccessfulApiCommand());
    }

    private void postClusterCommandsRestart() {
        dynamicRouteStack.post(CLUSTER_COMMANDS_RESTART,
                (request, response) ->
                        getSuccessfulApiCommand());
    }

    private void postBeginTrial() {
        dynamicRouteStack.post(BEGIN_FREE_TRIAL, (request, response) -> null);
    }

    private void addManagementService() {
        dynamicRouteStack.put(MANAGEMENT_SERVICE, (request, response) -> new ApiService());
    }

    private void getManagementService() {
        dynamicRouteStack.get(MANAGEMENT_SERVICE, (request, response) -> new ApiService().serviceState(ApiServiceState.STARTED));
    }

    private void startManagementService() {
        dynamicRouteStack.post(START_MANAGEMENT_SERVICE, (request, response) -> new ApiService());
    }

    private void listRoleTypes() {
        dynamicRouteStack.get(ROLE_TYPES, (request, response) -> new ApiRoleTypeList().items(new ArrayList<>()));
    }

    private void listRoles() {
        dynamicRouteStack.get(ROLES, (request, response) -> new ApiRoleTypeList().items(new ArrayList<>()));
    }

    private void createRoles() {
        dynamicRouteStack.post(ROLES, (request, response) -> new ApiRoleTypeList().items(new ArrayList<>()));
    }

    private void listActiveCommands() {
        dynamicRouteStack.get(ACTIVE_COMMANDS,
                (request, response) -> new ApiCommandList().items(
                        List.of(new ApiCommand().id(new BigDecimal(1)).active(Boolean.FALSE).success(Boolean.TRUE))));
    }

    private void listCommands() {
        dynamicRouteStack.get(LIST_COMMANDS,
                (request, response) -> new ApiCommandList().items(
                        List.of(new ApiCommand().id(new BigDecimal(1)).active(Boolean.FALSE).success(Boolean.TRUE))));
    }

    private void cmConfig() {
        dynamicRouteStack.get(CONFIG, (request, response) -> new ApiConfigList().items(new ArrayList<>()));
    }

    private void updateCmConfig() {
        dynamicRouteStack.put(CONFIG, (request, response) -> new ApiConfigList().items(new ArrayList<>()));
    }

    private void getCdpRemoteContext() {
        dynamicRouteStack.get(CDP_REMOTE_CONTEXT_BY_CLUSTER_CLUSTER_NAME,
                (req, res) -> new ApiRemoteDataContext());
    }

    private void postCdpRemoteContext() {
        dynamicRouteStack.post(CDP_REMOTE_CONTEXT,
                (req, res) -> new ApiRemoteDataContext());
    }

    private void getHosts() {
        dynamicRouteStack.get(HOSTS, (request, response, model) -> getHosts(model));
    }

    private void getHostById() {
        dynamicRouteStack.get(HOST_BY_ID, (request, response, model) ->
            getApiHost(model.getInstanceMap().get(request.params("hostId")))
        );
    }

    private void deleteHostById() {
        dynamicRouteStack.delete(HOST_BY_ID, (request, response, model) ->
                getApiHost(model.getInstanceMap().get(request.params("hostId")))
        );
    }

    private ApiHostList getHosts(DefaultModel model) {
        Map<String, CloudVmMetaDataStatus> instanceMap = model.getInstanceMap();
        ApiHostList apiHostList = new ApiHostList();
        for (Map.Entry<String, CloudVmMetaDataStatus> entry : instanceMap.entrySet()) {
            ApiHost apiHost = getApiHost(entry.getValue());
            apiHostList.addItemsItem(apiHost);
        }
        return apiHostList;
    }

    private ApiHost getApiHost(CloudVmMetaDataStatus cloudVmMetaDataStatus) {
        return new ApiHost()
                .hostId(cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getInstanceId())
                .hostname(HostNameUtil.generateHostNameByIp(cloudVmMetaDataStatus.getMetaData().getPrivateIp()))
                .ipAddress(cloudVmMetaDataStatus.getMetaData().getPrivateIp())
                .healthSummary(ApiHealthSummary.GOOD);
    }

}
