package com.sequenceiq.it.cloudbreak.mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiHostRefList;
import com.cloudera.api.swagger.model.ApiHostTemplateList;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.mock.model.ClouderaManagerMock;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
import com.sequenceiq.it.util.HostNameUtil;

public class SetupCmScalingMock {

    public static final String HOSTS_DECOMMISSION = ClouderaManagerMock.API_ROOT + "/cm/commands/hostsDecommission";

    public static final BigDecimal HOSTS_DECOMMISSION_COMMAND_ID = new BigDecimal(300);

    private static final String LIST_HOSTS = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/hosts";

    private static final String READ_HOSTS = ClouderaManagerMock.API_ROOT + "/hosts";

    private static final String READ_HOSTTEMPLATES = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/hostTemplates";

    private static final String ADD_HOSTS = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/hosts";

    private static final String LIST_CLUSTER_COMMANDS = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/commands";

    private static final String DEPLOY_CLIENT_CONFIG = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/commands/deployClientConfig";

    private static final String READ_PARCEL = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/parcels/products/:product/versions/:version";

    private static final String APPLY_HOST_TEMPLATE =
            ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/hostTemplates/:hostTemplateName/commands/applyHostTemplate";

    private static final String READ_HOST = ClouderaManagerMock.API_ROOT + "/hosts/:hostId";

    private static final String CLUSTERS_SERVICES = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/services";

    private static final String REMOVE_HOST = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/hosts/:hostId";

    private static final String DELETE_HOST = ClouderaManagerMock.API_ROOT + "/hosts/:hostId";

    private static final String DELETE_CREDENTIALS = ClouderaManagerMock.API_ROOT + "/cm/commands/deleteCredentials";

    private static final String RESTART_MGMTSERVCIES_COMMAND = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/commands/restart";

    private static final String RESTART_CLUSTER_COMMAND = ClouderaManagerMock.API_ROOT + "/cm/service/commands/restart";

    private static final BigDecimal DEPLOY_CLIENT_CONFIG_COMMAND_ID = new BigDecimal(100);

    private static final BigDecimal APPLY_HOST_TEMPLATE_COMMAND_ID = new BigDecimal(200);

    private Boolean firstListHostsRequest = Boolean.TRUE;

    private Integer originalWorkerCount;

    private Integer desiredWorkerCount;

    private Integer desiredBackscaledWorkerCount;

    private Stack<String> parcelStageResponses = new Stack<>();

    public void configure(MockedTestContext context, Integer originalWorkerCount, Integer desiredWorkerCount, Integer desiredBackscaledWorkerCount) {
        this.originalWorkerCount = originalWorkerCount;
        this.desiredWorkerCount = desiredWorkerCount;
        this.desiredBackscaledWorkerCount = desiredBackscaledWorkerCount;
        prepareReadParcelStageStack();
        addClouderaManagerMocks(context);
    }

    public Integer getOriginalWorkerCount() {
        return originalWorkerCount;
    }

    public Integer getDesiredWorkerCount() {
        return desiredWorkerCount;
    }

    public Integer getDesiredBackscaledWorkerCount() {
        return desiredBackscaledWorkerCount;
    }

    private void addClouderaManagerMocks(MockedTestContext testContext) {
        DynamicRouteStack dynamicRouteStack = testContext.getModel().getClouderaManagerMock().getDynamicRouteStack();

        List<ApiHost> apiHostList = generateHosts(testContext.getModel().getInstanceMap().values());
        List<ApiHostRef> apiHostRefList = generateHostsRefs(testContext.getModel().getInstanceMap().values());

        ApiHostRefList originalVms = new ApiHostRefList().items(generateHostsRefs(testContext.getModel().getInstanceMap().values().stream()
                .filter(status -> status.getCloudVmInstanceStatus().getCloudInstance().getTemplate().getPrivateId() <= originalWorkerCount)
                .collect(Collectors.toList())));
        ApiHostRefList upscaledVms = new ApiHostRefList().items(generateHostsRefs(testContext.getModel().getInstanceMap().values()));

        dynamicRouteStack.get(LIST_HOSTS,
                (request, response) -> {
                    if (firstListHostsRequest) {
                        firstListHostsRequest = Boolean.FALSE;
                        return originalVms;
                    } else {
                        return upscaledVms;
                    }
                });
        dynamicRouteStack.get(READ_HOSTS, (request, response) -> new ApiHostList().items(apiHostList));
        dynamicRouteStack.post(ADD_HOSTS, (request, response) -> new ApiHostRefList().items(apiHostRefList));
        dynamicRouteStack.post(DEPLOY_CLIENT_CONFIG, (request, response) -> new ApiCommand().id(DEPLOY_CLIENT_CONFIG_COMMAND_ID));
        dynamicRouteStack.get(READ_PARCEL, (request, response) -> new ApiParcel().stage(parcelStageResponses.pop()));
        dynamicRouteStack.post(APPLY_HOST_TEMPLATE, (request, response) -> new ApiCommand().id(APPLY_HOST_TEMPLATE_COMMAND_ID));
        dynamicRouteStack.get(READ_HOST,
                (request, response) -> {
                    String hostId = request.params(":hostId");
                    return apiHostList.stream()
                            .filter(host -> hostId.equals(host.getHostId()))
                            .findFirst().get();
                });
        dynamicRouteStack.post(HOSTS_DECOMMISSION, (request, response) -> new ApiCommand().id(HOSTS_DECOMMISSION_COMMAND_ID));
        dynamicRouteStack.get(CLUSTERS_SERVICES, (request, response) -> new ApiServiceList().items(List.of()));
        dynamicRouteStack.delete(REMOVE_HOST, (request, response) -> new ApiHost());
        dynamicRouteStack.delete(DELETE_HOST, (request, response) -> new ApiHost());
        dynamicRouteStack.post(DELETE_CREDENTIALS, (request, response) -> new ApiCommand().id(new BigDecimal(1)));
        dynamicRouteStack.post(RESTART_MGMTSERVCIES_COMMAND, (request, response) -> new ApiCommand().id(new BigDecimal(1)));
        dynamicRouteStack.get(LIST_CLUSTER_COMMANDS, (request, response) -> new ApiCommandList().items(List.of()));
        dynamicRouteStack.post(RESTART_CLUSTER_COMMAND, (request, response) -> new ApiCommand().id(new BigDecimal(1)));
        dynamicRouteStack.get(READ_HOSTTEMPLATES, (request, response) -> new ApiHostTemplateList().items(List.of()));
    }

    private List<ApiHostRef> generateHostsRefs(Collection<CloudVmMetaDataStatus> cloudVmMetadataStatusList) {
        return cloudVmMetadataStatusList.stream()
                .filter(status -> InstanceStatus.STARTED.equals(status.getCloudVmInstanceStatus().getStatus()))
                .map(CloudVmMetaDataStatus::getMetaData)
                .map(CloudInstanceMetaData::getPrivateIp)
                .map(HostNameUtil::generateHostNameByIp)
                .map(hostname -> new ApiHostRef().hostname(hostname).hostId(hostname))
                .collect(Collectors.toList());
    }

    private List<ApiHost> generateHosts(Collection<CloudVmMetaDataStatus> cloudVmMetadataStatusList) {
        List<ApiHost> apiHosts = new ArrayList<>();
        for (CloudVmMetaDataStatus vmStatus : cloudVmMetadataStatusList) {
            if (InstanceStatus.STARTED.equals(vmStatus.getCloudVmInstanceStatus().getStatus())) {
                String ip = vmStatus.getMetaData().getPrivateIp();
                String hostname = HostNameUtil.generateHostNameByIp(ip);
                apiHosts.add(new ApiHost()
                        .hostname(hostname)
                        .hostId(hostname)
                        .ipAddress(ip)
                        .healthChecks(List.of(new ApiHealthCheck()
                                .name("HOST_SCM_HEALTH")
                                .summary(ApiHealthSummary.GOOD)))
                        .lastHeartbeat(Instant.now().plusSeconds(60000L).toString()));
            }
        }
        return apiHosts;
    }

    private void prepareReadParcelStageStack() {
        parcelStageResponses.push("ACTIVATED");
        parcelStageResponses.push("ACTIVATING");
        parcelStageResponses.push("DISTRIBUTING");
        parcelStageResponses.push("DISTRIBUTING");
        parcelStageResponses.push("ACTIVATING");
    }
}
