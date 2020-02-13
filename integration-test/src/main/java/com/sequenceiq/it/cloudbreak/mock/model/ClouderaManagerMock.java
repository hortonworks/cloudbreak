package com.sequenceiq.it.cloudbreak.mock.model;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.model.ApiAuthRoleMetadataList;
import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiCluster;
import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiEcho;
import com.cloudera.api.swagger.model.ApiHealthCheck;
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
import com.cloudera.api.swagger.model.ApiVersionInfo;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.cloudbreak.mock.AbstractModelMock;
import com.sequenceiq.it.cloudbreak.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.mock.ProfileAwareRoute;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
import com.sequenceiq.it.util.HostNameUtil;

import spark.Service;

public class ClouderaManagerMock extends AbstractModelMock {

    public static final String PROFILE_RETURN_HTTP_500 = "cmHttp500";

    public static final String API_V31 = "/api/v31";

    public static final String API_V40 = "/api/v40";

    public static final String ECHO = API_V31 + "/tools/echo";

    public static final String USERS = API_V31 + "/users";

    public static final String USERS_USER = API_V31 + "/users/:user";

    public static final String IMPORT_CLUSTERTEMPLATE = "/api/v40/cm/importClusterTemplate";

    public static final String COMMANDS_COMMAND = API_V31 + "/commands/:commandId";

    public static final String COMMANDS_COMMAND_V40 = API_V40 + "/commands/:commandId";

    public static final String COMMANDS_STOP = API_V31 + "/clusters/:cluster/commands/stop";

    public static final String COMMANDS_START = API_V31 + "/clusters/:cluster/commands/start";

    public static final String COMMANDS = API_V31 + "/clusters/:clusterName/commands";

    public static final String CLUSTER_DEPLOY_CLIENT_CONFIG = API_V31 + "/clusters/:clusterName/commands/deployClientConfig";

    public static final String CM_REFRESH_PARCELREPOS = API_V31 + "/cm/commands/refreshParcelRepos";

    public static final String CM_REFRESH_PARCELREPOS_V40 = API_V40 + "/cm/commands/refreshParcelRepos";

    public static final String CLUSTER_PARCELS = API_V31 + "/clusters/:clusterName/parcels";

    public static final String CLUSTER_SERVICES = API_V31 + "/clusters/:clusterName/services";

    public static final String CLUSTER_HOSTS = API_V31 + "/clusters/:clusterName/hosts";

    public static final String CLUSTER_HOSTS_BY_HOSTID = API_V31 + "/clusters/:clusterName/hosts/:hostId";

    public static final String CLUSTER_SERVICE_ROLES = API_V31 + "/clusters/:clusterName/services/:serviceName/roles";

    public static final String CLUSTER_SERVICE_ROLES_BY_ROLE = API_V31 + "/clusters/:clusterName/services/:serviceName/roles/:roleName";

    public static final String CLUSTER_HOSTTEMPLATES = API_V31 + "/clusters/:clusterName/hostTemplates";

    public static final String CLUSTER_COMMANDS_REFRESH = API_V31 + "/clusters/:clusterName/commands/refresh";

    public static final String CLUSTER_COMMANDS_RESTART = API_V31 + "/clusters/:clusterName/commands/restart";

    public static final String HOSTS = API_V31 + "/hosts";

    public static final String HOSTS_V40 = API_V40 + "/hosts";

    public static final String HOST_BY_ID = API_V31 + "/hosts/:hostId";

    public static final String BEGIN_FREE_TRIAL = API_V31 + "/cm/trial/begin";

    public static final String MANAGEMENT_SERVICE = API_V31 + "/cm/service";

    public static final String MANAGEMENT_SERVICE_V40 = API_V40 + "/cm/service";

    public static final String START_MANAGEMENT_SERVICE = API_V31 + "/cm/service/commands/start";

    public static final String ACTIVE_COMMANDS = API_V31 + "/cm/service/commands";

    public static final String ROLE_TYPES = API_V31 + "/cm/service/roleTypes";

    public static final String ROLE_TYPES_V40 = API_V40 + "/cm/service/roleTypes";

    public static final String ROLES = API_V31 + "/cm/service/roles";

    public static final String ROLES_V40 = API_V40 + "/cm/service/roles";

    public static final String CONFIG = API_V31 + "/cm/config";

    public static final String CONFIG_V40 = API_V40 + "/cm/config";

    public static final String CM_RESTART = API_V31 + "/cm/service/commands/restart";

    public static final String CM_HOST_DECOMMISSION = API_V31 + "/cm/commands/hostsDecommission";

    public static final String CM_DELETE_CREDENTIALS = API_V31 + "/cm/commands/deleteCredentials";

    public static final String LIST_COMMANDS = API_V31 + "/cm/commands";

    public static final String LIST_COMMANDS_V40 = API_V40 + "/cm/commands";

    public static final String READ_AUTH_ROLES = API_V31 + "/authRoles/metadata";

    public static final String CDP_REMOTE_CONTEXT_BY_CLUSTER_CLUSTER_NAME = "/api/cdp/remoteContext/byCluster/:clusterName";

    public static final String CDP_REMOTE_CONTEXT = "/api/cdp/remoteContext";

    public static final String CM_VERSION = API_V31 + "/cm/version";

    public static final String CM_VERSION_V40 = API_V40 + "/cm/version";

    private static final String AUTO_CONFIGURE_COMMAND = API_V31 + "/cm/service/autoConfigure";

    private static final String AUTO_CONFIGURE_COMMAND_V40 = API_V40 + "/cm/service/autoConfigure";

    private static final String CONFIGURE_KERBEROS = API_V31 + "/clusters/:clusterName/commands/configureForKerberos";

    private static final String GENERATE_CREDENTIALS = API_V31 + "/cm/commands/generateCredentials";

    private static final String CLUSTER = API_V40 + "/clusters/:clusterName";

    private DynamicRouteStack dynamicRouteStack;

    private final Set<String> activeProfiles;

    public ClouderaManagerMock(Service sparkService, DefaultModel defaultModel, Set<String> activeProfiles) {
        super(sparkService, defaultModel);
        dynamicRouteStack = new DynamicRouteStack(sparkService, defaultModel);
        this.activeProfiles = activeProfiles;
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
        getCommand(COMMANDS_COMMAND);
        getCommand(COMMANDS_COMMAND_V40);
        postStopCommand();
        getHosts(HOSTS);
        getHosts(HOSTS_V40);
        postStartCommand();
        postBeginTrial();
        addManagementService(MANAGEMENT_SERVICE);
        addManagementService(MANAGEMENT_SERVICE_V40);
        getManagementService(MANAGEMENT_SERVICE);
        getManagementService(MANAGEMENT_SERVICE_V40);
        createRoles(ROLES);
        createRoles(ROLES_V40);
        listRoleTypes(ROLE_TYPES);
        listRoleTypes(ROLE_TYPES_V40);
        listRoles(ROLES_V40);
        cmConfig(CONFIG);
        updateCmConfig(CONFIG);
        cmConfig(CONFIG_V40);
        updateCmConfig(CONFIG_V40);
        getCmVersion(CM_VERSION);
        getCmVersion(CM_VERSION_V40);
        startManagementService();
        listCommands(LIST_COMMANDS);
        listCommands(LIST_COMMANDS_V40);
        listActiveCommands();
        readAuthRoles();
        getCdpRemoteContext();
        postCdpRemoteContext();

        getClusterServices();
        getClusterHosts();
        postCMRefreshParcelRepos(CM_REFRESH_PARCELREPOS);
        postCMRefreshParcelRepos(CM_REFRESH_PARCELREPOS_V40);
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
        putAutoConfigure(AUTO_CONFIGURE_COMMAND);
        putAutoConfigure(AUTO_CONFIGURE_COMMAND_V40);
        dynamicRouteStack.post(CONFIGURE_KERBEROS, new ProfileAwareRoute(
                (request, response) -> new ApiCommand().id(BigDecimal.ONE).active(Boolean.TRUE).name("Configure Kerberos"), activeProfiles));
        dynamicRouteStack.post(GENERATE_CREDENTIALS, new ProfileAwareRoute(
                (request, response) -> new ApiCommand().id(BigDecimal.ONE).active(Boolean.TRUE).name("Generate Credentials"), activeProfiles));
    }

    private void readAuthRoles() {
        dynamicRouteStack.get(READ_AUTH_ROLES,
                new ProfileAwareRoute((request, response) -> new ApiAuthRoleMetadataList(), activeProfiles));
    }

    private void getEcho() {
        dynamicRouteStack.get(ECHO, new ProfileAwareRoute((request, response) -> {
            String message = request.queryMap("message").value();
            message = message == null ? "Hello World!" : message;
            return new ApiEcho().message(message);
        }, activeProfiles));
    }

    private void getUsers() {
        dynamicRouteStack.get(USERS, new ProfileAwareRoute((request, response) -> getUserList(), activeProfiles));
    }

    private void putUser() {
        dynamicRouteStack.put(USERS_USER, new ProfileAwareRoute((request, response)
                -> new ApiUser2().name(request.params("user")), activeProfiles));
    }

    private void postUser() {
        dynamicRouteStack.post(USERS, new ProfileAwareRoute((request, response) -> getUserList(), activeProfiles));
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
                new ProfileAwareRoute((request, response, model) -> {
                    ApiClient client = new ApiClient();

                    Type type = ApiClusterTemplate.class;
                    ApiClusterTemplate template = client.getJSON().deserialize(request.body(), type);
                    model.setClouderaManagerProducts(template.getProducts());

                    return new ApiCommand().id(BigDecimal.ONE).name("Import ClusterTemplate").active(Boolean.TRUE);
                }, activeProfiles));
    }

    private void postClouderaManagerHostDecommission() {
        dynamicRouteStack.post(CM_HOST_DECOMMISSION, new ProfileAwareRoute((request, response)
                -> getSuccessfulApiCommand(), activeProfiles));
    }

    private void postClouderaManagerDeleteCredentials() {
        dynamicRouteStack.post(CM_DELETE_CREDENTIALS, new ProfileAwareRoute((request, response)
                -> getSuccessfulApiCommand(), activeProfiles));
    }

    private void postClouderaManagerRestart() {
        dynamicRouteStack.post(CM_RESTART, new ProfileAwareRoute((request, response)
                -> getSuccessfulApiCommand(), activeProfiles));
    }

    private void getCommand(String endpoint) {
        dynamicRouteStack.get(endpoint, new ProfileAwareRoute((request, response)
                -> new ApiCommand().id(new BigDecimal(request.params("commandId"))).active(Boolean.FALSE).success(Boolean.TRUE), activeProfiles));
    }

    private void getCommands() {
        dynamicRouteStack.get(COMMANDS, new ProfileAwareRoute((request, response)
                -> new ApiCommandList().items(List.of(new ApiCommand().name("something"))), activeProfiles));
    }

    private void postStopCommand() {
        dynamicRouteStack.post(COMMANDS_STOP, new ProfileAwareRoute((request, response)
                -> new ApiCommand().id(BigDecimal.ONE).active(Boolean.TRUE).name("Stop"), activeProfiles));
    }

    private void postStartCommand() {
        dynamicRouteStack.post(COMMANDS_START, new ProfileAwareRoute((request, response)
                -> new ApiCommand().id(BigDecimal.ONE).active(Boolean.TRUE).name("Start"), activeProfiles));
    }

    private void getClusterServices() {
        dynamicRouteStack.get(CLUSTER_SERVICES, new ProfileAwareRoute((request, response)
                -> new ApiServiceList().items(List.of(new ApiService().name("service1").serviceState(ApiServiceState.STARTED))), activeProfiles));
    }

    private void getClusterHosts() {
        dynamicRouteStack.get(CLUSTER_HOSTS, new ProfileAwareRoute((request, response, model) -> getHosts(model), activeProfiles));
    }

    private void deleteClusterHosts() {
        dynamicRouteStack.delete(CLUSTER_HOSTS_BY_HOSTID,
                new ProfileAwareRoute((request, response, model) -> getHosts(model), activeProfiles));
    }

    private void getCluster() {
        dynamicRouteStack.get(CLUSTER,
                new ProfileAwareRoute((request, response, model) -> new ApiCluster(), activeProfiles));
    }

    private void getClusterHostTemplates() {
        dynamicRouteStack.get(CLUSTER_HOSTTEMPLATES, new ProfileAwareRoute((request, response, model) -> {
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
        }, activeProfiles));
    }

    private void getClusterServiceRoles() {
        dynamicRouteStack.get(CLUSTER_SERVICE_ROLES, new ProfileAwareRoute((request, response)
                -> new ApiRoleList().items(List.of(new ApiRole().name("role1").serviceRef(new ApiServiceRef().serviceName("service1"))
        )), activeProfiles));
    }

    private void deleteClusterServiceRole() {
        dynamicRouteStack.delete(CLUSTER_SERVICE_ROLES_BY_ROLE, new ProfileAwareRoute((request, response)
                -> new ApiRole().name("role1"), activeProfiles));
    }

    private ApiCommand getSuccessfulApiCommand() {
        ApiCommand result = new ApiCommand();
        result.setId(BigDecimal.valueOf(1L));
        result.setActive(true);
        result.setSuccess(true);
        return result;
    }

    private void postCMRefreshParcelRepos(String endpoint) {
        dynamicRouteStack.post(endpoint, new ProfileAwareRoute((request, response) -> getSuccessfulApiCommand(), activeProfiles));
    }

    private void getClusterParcels() {
        dynamicRouteStack.get(CLUSTER_PARCELS,
                new ProfileAwareRoute((request, response, model) -> {
                    List<ApiProductVersion> products = model.getClouderaManagerProducts();

                    return new ApiParcelList().items(
                            products.stream().map(product -> new ApiParcel()
                                    .product(product.getProduct())
                                    .version(product.getVersion())
                                    .stage("ACTIVATED"))
                                    .collect(Collectors.toList())
                    );
                }, activeProfiles));
    }

    private void postClusterDeployClientConfig() {
        dynamicRouteStack.post(CLUSTER_DEPLOY_CLIENT_CONFIG, new ProfileAwareRoute((request, response)
                -> getSuccessfulApiCommand(), activeProfiles));
    }

    private void postClusterCommandsRefresh() {
        dynamicRouteStack.post(CLUSTER_COMMANDS_REFRESH, new ProfileAwareRoute((request, response)
                -> getSuccessfulApiCommand(), activeProfiles));
    }

    private void postClusterCommandsRestart() {
        dynamicRouteStack.post(CLUSTER_COMMANDS_RESTART, new ProfileAwareRoute((request, response)
                -> getSuccessfulApiCommand(), activeProfiles));
    }

    private void postBeginTrial() {
        dynamicRouteStack.post(BEGIN_FREE_TRIAL, new ProfileAwareRoute((request, response) -> null, activeProfiles));
    }

    private void addManagementService(String endpoint) {
        dynamicRouteStack.put(endpoint, new ProfileAwareRoute((request, response) -> new ApiService(), activeProfiles));
    }

    private void getManagementService(String endpoint) {
        dynamicRouteStack.get(endpoint, new ProfileAwareRoute((request, response)
                -> new ApiService().serviceState(ApiServiceState.STARTED), activeProfiles));
    }

    private void startManagementService() {
        dynamicRouteStack.post(START_MANAGEMENT_SERVICE, new ProfileAwareRoute((request, response) -> new ApiService(), activeProfiles));
    }

    private void listRoleTypes(String endpoint) {
        dynamicRouteStack.get(endpoint, new ProfileAwareRoute((request, response)
                -> new ApiRoleTypeList().items(new ArrayList<>()), activeProfiles));
    }

    private void listRoles(String endpoint) {
        dynamicRouteStack.get(endpoint, new ProfileAwareRoute((request, response)
                -> new ApiRoleTypeList().items(new ArrayList<>()), activeProfiles));
    }

    private void createRoles(String endpoint) {
        dynamicRouteStack.post(endpoint, new ProfileAwareRoute((request, response)
                -> new ApiRoleTypeList().items(new ArrayList<>()), activeProfiles));
    }

    private void listActiveCommands() {
        dynamicRouteStack.get(ACTIVE_COMMANDS, new ProfileAwareRoute((request, response) -> new ApiCommandList().items(
                List.of(new ApiCommand().id(new BigDecimal(1)).active(Boolean.FALSE).success(Boolean.TRUE))), activeProfiles));
    }

    private void listCommands(String endpoint) {
        dynamicRouteStack.get(endpoint, new ProfileAwareRoute((request, response) -> new ApiCommandList().items(
                List.of(new ApiCommand().id(new BigDecimal(1)).active(Boolean.FALSE).success(Boolean.TRUE))), activeProfiles));
    }

    private void cmConfig(String endpoint) {
        dynamicRouteStack.get(endpoint, new ProfileAwareRoute((request, response)
                -> new ApiConfigList().items(new ArrayList<>()), activeProfiles));
    }

    private void updateCmConfig(String endpoint) {
        dynamicRouteStack.put(endpoint, new ProfileAwareRoute((request, response)
                -> new ApiConfigList().items(new ArrayList<>()), activeProfiles));
    }

    private void getCmVersion(String endpoint) {
        dynamicRouteStack.get(endpoint, new ProfileAwareRoute((request, response)
                -> new ApiVersionInfo().version("7.0.1"), activeProfiles));
    }

    private void getCdpRemoteContext() {
        dynamicRouteStack.get(CDP_REMOTE_CONTEXT_BY_CLUSTER_CLUSTER_NAME,
                new ProfileAwareRoute((req, res) -> new ApiRemoteDataContext(), activeProfiles));
    }

    private void postCdpRemoteContext() {
        dynamicRouteStack.post(CDP_REMOTE_CONTEXT,
                new ProfileAwareRoute((req, res) -> new ApiRemoteDataContext(), activeProfiles));
    }

    private void getHosts(String endpoint) {
        dynamicRouteStack.get(endpoint, new ProfileAwareRoute((request, response, model) -> {
            Map<String, CloudVmMetaDataStatus> instanceMap = model.getInstanceMap();
            ApiHostList apiHostList = new ApiHostList();
            for (Map.Entry<String, CloudVmMetaDataStatus> entry : instanceMap.entrySet()) {
                ApiHost apiHost = new ApiHost()
                        .hostId(entry.getValue().getCloudVmInstanceStatus().getCloudInstance().getInstanceId())
                        .hostname(HostNameUtil.generateHostNameByIp(entry.getValue().getMetaData().getPrivateIp()))
                        .ipAddress(entry.getValue().getMetaData().getPrivateIp())
                        .healthChecks(List.of(new ApiHealthCheck()
                                .name("HOST_SCM_HEALTH")
                                .summary(ApiHealthSummary.GOOD)))
                        .lastHeartbeat(Instant.now().toString());
                apiHostList.addItemsItem(apiHost);
            }
            return apiHostList;
        }, activeProfiles));
        dynamicRouteStack.get(endpoint, new ProfileAwareRoute((request, response, model) -> getHosts(model), activeProfiles));
    }

    private void getHostById() {
        dynamicRouteStack.get(HOST_BY_ID, new ProfileAwareRoute((request, response, model)
                -> getApiHost(model.getInstanceMap().get(request.params("hostId"))), activeProfiles));
    }

    private void deleteHostById() {
        dynamicRouteStack.delete(HOST_BY_ID,
                new ProfileAwareRoute((request, response, model) -> getApiHost(model.getInstanceMap().get(request.params("hostId"))), activeProfiles));
    }

    private void putAutoConfigure(String endpoint) {
        dynamicRouteStack.put(endpoint, (request, response) -> new ApiCommand().id(new BigDecimal(2)));

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
                .lastHeartbeat(Instant.now().plusSeconds(60000L).toString())
                .healthSummary(ApiHealthSummary.GOOD)
                .healthChecks(List.of(new ApiHealthCheck()
                        .name("HOST_SCM_HEALTH")
                        .summary(ApiHealthSummary.GOOD)));
    }
}
