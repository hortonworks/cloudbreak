package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cli.cm.ClusterToClouderaManagerV4RequestConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.CloudStorageConverter;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Component
public class ClusterToClusterV4RequestConverter {

    @Inject
    private ClusterToClouderaManagerV4RequestConverter clouderaManagerV4RequestConverter;

    @Inject
    private CloudStorageConverter cloudStorageConverter;

    @Inject
    private GatewayToGatewayV4RequestConverter gatewayToGatewayV4RequestConverter;

    public ClusterV4Request convert(Cluster source) {
        ClusterV4Request clusterRequest = new ClusterV4Request();
        convertClusterManager(source, clusterRequest);
        clusterRequest.setBlueprintName(source.getBlueprint().getName());
        clusterRequest.setValidateBlueprint(null);
        clusterRequest.setUserName("");
        clusterRequest.setPassword("");
        if (source.getFileSystem() != null) {
            clusterRequest.setCloudStorage(cloudStorageConverter.fileSystemToRequest(source.getFileSystem()));
        }
        clusterRequest.setName(source.getName());
        if (source.getRdsConfigs() != null && !source.getRdsConfigs().isEmpty()) {
            Set<String> databaseNames = source.getRdsConfigs().stream()
                    .filter(rdsConfig -> rdsConfig.getStatus() == ResourceStatus.USER_MANAGED)
                    .map(RDSConfig::getName)
                    .collect(Collectors.toSet());
            clusterRequest.setDatabases(Collections.unmodifiableSet(databaseNames));
        }

        if (StringUtils.isNotEmpty(source.getProxyConfigCrn())) {
            clusterRequest.setProxyConfigCrn(source.getProxyConfigCrn());
        }

        if (source.getGateway() != null) {
            clusterRequest.setGateway(gatewayToGatewayV4RequestConverter.convert(source.getGateway()));
        }
        clusterRequest.setRangerRazEnabled(source.isRangerRazEnabled());
        clusterRequest.setRangerRmsEnabled(source.isRangerRmsEnabled());
        clusterRequest.setEncryptionProfileName(source.getEncryptionProfileName());
        return clusterRequest;
    }

    private void convertClusterManager(Cluster cluster, ClusterV4Request clusterRequest) {
        clusterRequest.setCm(clouderaManagerV4RequestConverter.convert(cluster));
    }
}
