package com.sequenceiq.it.cloudbreak.newway.mock.model;

import static com.sequenceiq.it.cloudbreak.newway.Mock.responseFromJsonFile;
import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.cloudbreak.newway.mock.AbstractModelMock;
import com.sequenceiq.it.cloudbreak.newway.mock.DefaultModel;
import com.sequenceiq.it.spark.DynamicRouteStack;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.spark.StatefulRoute;
import com.sequenceiq.it.spark.ambari.AmbariCheckResponse;
import com.sequenceiq.it.spark.ambari.AmbariClusterRequestsResponse;
import com.sequenceiq.it.spark.ambari.AmbariClusterResponse;
import com.sequenceiq.it.spark.ambari.AmbariClustersHostsResponseW;
import com.sequenceiq.it.spark.ambari.AmbariComponentStatusOnHostResponse;
import com.sequenceiq.it.spark.ambari.AmbariHostsResponseV2;
import com.sequenceiq.it.spark.ambari.AmbariServiceConfigResponseV2;
import com.sequenceiq.it.spark.ambari.AmbariServicesComponentsResponse;
import com.sequenceiq.it.spark.ambari.AmbariStatusResponse;
import com.sequenceiq.it.spark.ambari.AmbariVersionDefinitionResponse;
import com.sequenceiq.it.spark.ambari.AmbariViewResponse;
import com.sequenceiq.it.spark.ambari.EmptyAmbariClusterResponse;
import com.sequenceiq.it.spark.ambari.EmptyAmbariResponse;
import com.sequenceiq.it.spark.ambari.v2.AmbariCategorizedHostComponentStateResponse;
import com.sequenceiq.it.util.HostNameUtil;

import spark.Service;

public class AmbariMock extends AbstractModelMock {

    public static final String CLUSTERS = AMBARI_API_ROOT + "/clusters";

    public static final String CLUSTERS_CLUSTER = AMBARI_API_ROOT + "/clusters/:cluster";

    public static final String CLUSTERS_CLUSTER_NAME_CONFIGURATIONS_SERVICE_CONFIG_VERSIONS = CLUSTERS_CLUSTER + "/configurations/service_config_versions";

    public static final String CLUSTERS_CLUSTER_HOSTS = CLUSTERS_CLUSTER + "/hosts";

    public static final String CLUSTERS_CLUSTER_HOSTS_HOSTNAME_HOST_COMPONENTS = CLUSTERS_CLUSTER + "/hosts/:hostname/host_components/*";

    public static final String CLUSTERS_CLUSTER_HOSTS_INTERNALHOSTNAME = CLUSTERS_CLUSTER + "/hosts/:internalhostname";

    public static final String CLUSTERS_CLUSTER_HOST_COMPONENTS = CLUSTERS_CLUSTER + "/host_components";

    public static final String CLUSTERS_CLUSTER_HOSTS_HOSTNAME = CLUSTERS_CLUSTER + "/hosts/:hostname";

    public static final String CLUSTERS_CLUSTER_SERVICES = CLUSTERS_CLUSTER + "/services/*";

    public static final String CLUSTERS_CLUSTER_SERVICES_HDFS_COMPONENTS_NAMENODE = CLUSTERS_CLUSTER + "/services/HDFS/components/NAMENODE";

    public static final String CLUSTERS_CLUSTER_REQUESTS_REQUEST = CLUSTERS_CLUSTER + "/requests/:request";

    public static final String CLUSTERS_CLUSTER_REQUESTS = CLUSTERS_CLUSTER + "/requests";

    public static final String STACKS_HDP_VERSIONS_VERSION_OPERATING_SYSTEMS_OS_REPOSITORIES_HDPVERSION
            = AMBARI_API_ROOT + "/stacks/HDP/versions/:version/operating_systems/:os/repositories/:hdpversion";

    public static final String USERS = AMBARI_API_ROOT + "/users";

    public static final String USERS_ADMIN = AMBARI_API_ROOT + "/users/admin";

    public static final String BLUEPRINTS = AMBARI_API_ROOT + "/blueprints/*";

    public static final String BLUEPRINTS_BLUEPRINTNAME = AMBARI_API_ROOT + "/blueprints/:blueprintname";

    public static final String SERVICES_AMBARI_COMPONENTS_AMBARI_SERVER = AMBARI_API_ROOT + "/services/AMBARI/components/AMBARI_SERVER";

    public static final String VIEWS_VIEW_VERSIONS_1_0_0_INSTANCES = AMBARI_API_ROOT + "/views/:view/versions/1.0.0/instances/*";

    public static final String CHECK = AMBARI_API_ROOT + "/check";

    public static final String VIEWS = AMBARI_API_ROOT + "/views/*";

    public static final String VERSION_DEFINITIONS = AMBARI_API_ROOT + "/version_definitions";

    public static final String HOSTS = AMBARI_API_ROOT + "/hosts";

    private DynamicRouteStack dynamicRouteStack;

    public AmbariMock(Service sparkService, DefaultModel defaultModel) {
        super(sparkService, defaultModel);
        dynamicRouteStack = new DynamicRouteStack(sparkService, defaultModel);
    }

    public void addAmbariMappings() {
        getAmbariClusterRequest();
        getAmbariClusters();
        postAmbariClusterRequest();
        getAmbariCheck();
        postAmbariUsers();
        getAmbariBlueprint();
        getAmbariClusterHosts("STARTED");
        getAmbariHosts();
        postAmbariInstances();
        postAmbariClusters();
        getAmbariComponents();
        postAmbariBlueprints();
        putAmbariUsersAdmin();
        getAmbariClusterHosts();
        putAmbariHdpVersion();
        getAmabriVersionDefinitions();
        postAmbariVersionDefinitions();

        getAmbariCluster();
        getAmbariClusterHosts("INSTALLED");
        putAmbariClusterServices();
        postAmbariClusterHosts();
        getAmbariClusterHostComponents();
        getAmbariClusterConfigurationVersions();
        getAmbariClusterHostStatus();
        getAmbariClusterServicesComponentsNamenode();
        putAmbariClusterHostComponents();
        deleteClusterHostComponents();
        deleteAmbariClusterHost();
        getAmbariViews();
    }

    public DynamicRouteStack getDynamicRouteStack() {
        return dynamicRouteStack;
    }

    private void postAmbariClusters() {
        dynamicRouteStack.post(CLUSTERS_CLUSTER, (request, respponse, model) -> {
            model.setClusterName(request.params("cluster"));
            model.setClusterCreated(true);
            return "";
        });
    }

    private void getAmabriVersionDefinitions() {
        dynamicRouteStack.get(VERSION_DEFINITIONS, new AmbariVersionDefinitionResponse());
    }

    private void getAmbariClusterHosts() {
        dynamicRouteStack.get(CLUSTERS_CLUSTER_HOSTS, (StatefulRoute) new AmbariCategorizedHostComponentStateResponse());
    }

    private void getAmbariClusterHosts2() {
        getAmbariClusterHosts("INSTALLED");
    }

    private void getAmbariClusterHosts(String state) {
        dynamicRouteStack.get(CLUSTERS_CLUSTER_HOSTS, (StatefulRoute) new AmbariClustersHostsResponseW(state));
    }

    private void getAmbariHosts() {
        dynamicRouteStack.get(HOSTS, new AmbariHostsResponseV2());
    }

    public void getAmbariClusters() {
        dynamicRouteStack.get(CLUSTERS, (req, resp, model) -> {
            ITResponse itResp = model.isClusterCreated()
                    ? new AmbariClusterResponse(model.getInstanceMap(), model.getClusterName()) : new EmptyAmbariClusterResponse();
            return itResp.handle(req, resp);
        });
    }

    private void getAmbariClusterServicesComponentsNamenode() {
        dynamicRouteStack.get(CLUSTERS_CLUSTER_SERVICES_HDFS_COMPONENTS_NAMENODE, (request, response, model) -> {
            response.type("text/plain");
            ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
            ObjectNode nameNode = rootNode.putObject("metrics").putObject("dfs").putObject("namenode");
            ObjectNode liveNodesRoot = JsonNodeFactory.instance.objectNode();

            for (CloudVmMetaDataStatus status : model.getInstanceMap().values()) {
                ObjectNode node = liveNodesRoot.putObject(HostNameUtil.generateHostNameByIp(status.getMetaData().getPrivateIp()));
                node.put("remaining", "10000000");
                node.put("usedSpace", Integer.toString(100000));
                node.put("adminState", "In Service");
            }

            nameNode.put("LiveNodes", liveNodesRoot.toString());
            nameNode.put("DecomNodes", "{}");
            return rootNode;
        });
    }

    private void getAmbariClusterHostStatus() {
        dynamicRouteStack.get(CLUSTERS_CLUSTER_HOSTS_INTERNALHOSTNAME, (request, response) -> {
            response.type("text/plain");
            ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
            rootNode.putObject("Hosts").put("public_host_name", request.params("internalhostname")).put("host_status", "HEALTHY");
            return rootNode;
        });
    }

    private void getAmbariClusterConfigurationVersions() {
        dynamicRouteStack.get(CLUSTERS_CLUSTER_NAME_CONFIGURATIONS_SERVICE_CONFIG_VERSIONS,
                new AmbariServiceConfigResponseV2());
    }

    private void getAmbariClusterHostComponents() {
        dynamicRouteStack.get(CLUSTERS_CLUSTER_HOSTS_HOSTNAME_HOST_COMPONENTS,
                new AmbariComponentStatusOnHostResponse());
    }

    private void postAmbariClusterHosts() {
        dynamicRouteStack.post(CLUSTERS_CLUSTER_HOSTS, new AmbariClusterRequestsResponse());
    }

    private void putAmbariClusterServices() {
        dynamicRouteStack.put(CLUSTERS_CLUSTER_SERVICES, new AmbariClusterRequestsResponse());
    }

    public void getAmbariCluster() {
        dynamicRouteStack.get(CLUSTERS_CLUSTER, (StatefulRoute) new AmbariClusterResponse());
    }

    private void getAmbariViews() {
        dynamicRouteStack.get(VIEWS, new AmbariViewResponse(getDefaultModel().getMockServerAddress()));
    }

    private void postAmbariVersionDefinitions() {
        dynamicRouteStack.post(VERSION_DEFINITIONS, new EmptyAmbariResponse());
    }

    private void putAmbariHdpVersion() {
        dynamicRouteStack.put(STACKS_HDP_VERSIONS_VERSION_OPERATING_SYSTEMS_OS_REPOSITORIES_HDPVERSION, new AmbariVersionDefinitionResponse());
    }

    private void putAmbariClusterHostComponents() {
        dynamicRouteStack.put(CLUSTERS_CLUSTER_HOST_COMPONENTS, new AmbariClusterRequestsResponse());
    }

    private void deleteClusterHostComponents() {
        dynamicRouteStack.delete(CLUSTERS_CLUSTER_HOSTS_HOSTNAME_HOST_COMPONENTS, new EmptyAmbariResponse());
    }

    private void deleteAmbariClusterHost() {
        dynamicRouteStack.delete(CLUSTERS_CLUSTER_HOSTS_HOSTNAME, new AmbariClusterRequestsResponse());
    }

    private void postAmbariUsers() {
        dynamicRouteStack.post(USERS, new EmptyAmbariResponse());
    }

    private void putAmbariUsersAdmin() {
        dynamicRouteStack.put(USERS_ADMIN, new EmptyAmbariResponse());
    }

    private void postAmbariBlueprints() {
        dynamicRouteStack.post(BLUEPRINTS, new EmptyAmbariResponse());
    }

    private void getAmbariBlueprint() {
        dynamicRouteStack.get(BLUEPRINTS_BLUEPRINTNAME, (request, response) -> {
            response.type("text/plain");
            return responseFromJsonFile("blueprint/" + request.params("blueprintname") + ".bp");
        });
    }

    private void getAmbariComponents() {
        dynamicRouteStack.get(SERVICES_AMBARI_COMPONENTS_AMBARI_SERVER, new AmbariServicesComponentsResponse());
    }

    private void postAmbariCluster(Service sparkService) {
        dynamicRouteStack.post(CLUSTERS_CLUSTER, (request, response) -> {
            getDefaultModel().setClusterName(request.params("cluster"));
            response.type("text/plain");
            return "";
        });
    }

    private void postAmbariClusterRequest() {
        dynamicRouteStack.post(CLUSTERS_CLUSTER_REQUESTS, new AmbariClusterRequestsResponse());
    }

    private void postAmbariInstances() {
        dynamicRouteStack.post(VIEWS_VIEW_VERSIONS_1_0_0_INSTANCES, new EmptyAmbariResponse());
    }

    private void getAmbariClusterRequest() {
        dynamicRouteStack.get(CLUSTERS_CLUSTER_REQUESTS_REQUEST, new AmbariStatusResponse());
    }

    private void getAmbariCheck() {
        dynamicRouteStack.get(CHECK, new AmbariCheckResponse());
    }

}
