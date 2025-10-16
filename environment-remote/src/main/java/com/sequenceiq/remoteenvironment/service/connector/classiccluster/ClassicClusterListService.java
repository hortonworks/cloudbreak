package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.remotecluster.client.RemoteClusterServiceClient;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;

@Component
class ClassicClusterListService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassicClusterListService.class);

    private static final String PRIVATE_CLOUD = "PRIVATE_CLOUD";

    private static final String ENTITY_STATUS = "entityStatus";

    private static final String GOOD_HEALTH = "GOOD_HEALTH";

    private static final String AVAILABLE = "AVAILABLE";

    private static final String UNKNOWN = "UNKNOWN";

    @Inject
    private RemoteClusterServiceClient remoteClusterServiceClient;

    Collection<SimpleRemoteEnvironmentResponse> list() {
        List<OnPremisesApiProto.Cluster> clusters = remoteClusterServiceClient.listClassicClusters();
        return clusters.stream().map(this::convert).collect(Collectors.toSet());
    }

    private SimpleRemoteEnvironmentResponse convert(OnPremisesApiProto.Cluster cluster) {
        SimpleRemoteEnvironmentResponse response = new SimpleRemoteEnvironmentResponse();
        response.setCrn(cluster.getClusterCrn());
        response.setEnvironmentCrn(cluster.getEnvironmentCrn());
        response.setName(cluster.getName());
        response.setCreated(cluster.getLastCreateTime());
        response.setUrl(cluster.getKnoxEnabled() && StringUtils.isNotBlank(cluster.getKnoxUrl()) ? cluster.getKnoxUrl() : cluster.getManagerUri());
        response.setStatus(getStatus(cluster));
        response.setPrivateControlPlaneName(cluster.getDatacenterName());
        response.setCloudPlatform(PRIVATE_CLOUD);
        response.setRegion(PRIVATE_CLOUD);
        return response;
    }

    private String getStatus(OnPremisesApiProto.Cluster cluster) {
        Map<String, Object> clusterDataProperties = new Json(cluster.getData().getProperties()).getMap();
        return convertStatus(clusterDataProperties.get(ENTITY_STATUS));
    }

    private String convertStatus(Object status) {
        String convertedStatus;
        if (status instanceof String statusStr) {
            convertedStatus = GOOD_HEALTH.equals(statusStr) ? AVAILABLE : statusStr;
        } else {
            convertedStatus = UNKNOWN;
        }
        LOGGER.debug("Cluster status converted from {} to {}", status, convertedStatus);
        return convertedStatus;
    }
}
