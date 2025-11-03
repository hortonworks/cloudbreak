package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;

@Component
class ClassicClusterListService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassicClusterListService.class);

    private static final String PRIVATE_CLOUD = "PRIVATE_CLOUD";

    private static final String AVAILABLE = "AVAILABLE";

    Collection<SimpleRemoteEnvironmentResponse> list(List<OnPremisesApiProto.Cluster> clusters) {
        return clusters.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
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
        OnPremisesApiProto.EntityStatus.Value entityStatus = cluster.getData().getClusterDetails().getEntityStatus();
        String convertedStatus = OnPremisesApiProto.EntityStatus.Value.GOOD_HEALTH.equals(entityStatus) ? AVAILABLE : entityStatus.toString();
        LOGGER.debug("Cluster status converted from {} to {}", entityStatus, convertedStatus);
        return convertedStatus;
    }
}
