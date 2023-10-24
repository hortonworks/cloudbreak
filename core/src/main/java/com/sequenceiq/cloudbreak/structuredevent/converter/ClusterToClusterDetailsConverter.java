package com.sequenceiq.cloudbreak.structuredevent.converter;

import static java.lang.Boolean.FALSE;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigWithoutClusterService;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.GatewayView;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class ClusterToClusterDetailsConverter {

    @Inject
    private RdsConfigToRdsDetailsConverter rdsConfigToRdsDetailsConverter;

    @Inject
    private RdsConfigWithoutClusterService rdsConfigWithoutClusterService;

    public ClusterDetails convert(ClusterView source, StackView stack, GatewayView gatewayView) {
        ClusterDetails clusterDetails = new ClusterDetails();
        clusterDetails.setId(source.getId());
        clusterDetails.setName(source.getName());
        clusterDetails.setDescription(source.getDescription());
        if (stack != null && stack.getStatus() != null) {
            clusterDetails.setStatus(stack.getStatus().toString());
        }
        if (stack != null) {
            clusterDetails.setStatusReason(stack.getStatusReason());
        }
        clusterDetails.setCreationStarted(source.getCreationStarted());
        clusterDetails.setCreationFinished(source.getCreationFinished());
        clusterDetails.setUpSince(source.getUpSince());
        clusterDetails.setRazEnabled(source.isRangerRazEnabled());
        clusterDetails.setRmsEnabled(source.isRangerRmsEnabled());
        convertGatewayProperties(clusterDetails, gatewayView);
        convertFileSystemProperties(clusterDetails, source.getFileSystem());
        addDatabaseInfo(clusterDetails, source);
        return clusterDetails;
    }

    private void addDatabaseInfo(ClusterDetails clusterDetails, ClusterView source) {
        Set<RdsConfigWithoutCluster> rdsConfigs = rdsConfigWithoutClusterService.findByClusterId(source.getId());
        if (!CollectionUtils.isEmpty(rdsConfigs)) {
            clusterDetails.setDatabases(
                    rdsConfigs.stream()
                            .map(e -> rdsConfigToRdsDetailsConverter.convert(e))
                            .collect(Collectors.toList()));
            clusterDetails.setDbSslEnabled(Optional.ofNullable(source.getDbSslEnabled()).orElse(FALSE));
        }
    }

    private void convertGatewayProperties(ClusterDetails clusterDetails, GatewayView gateway) {
        if (gateway != null) {
            clusterDetails.setGatewayEnabled(true);
            clusterDetails.setGatewayType(gateway.getGatewayType().toString());
            clusterDetails.setSsoType(gateway.getSsoType().toString());
        } else {
            clusterDetails.setGatewayEnabled(false);
        }
    }

    private void convertFileSystemProperties(ClusterDetails clusterDetails, FileSystem fileSystem) {
        if (fileSystem != null && fileSystem.getType() != null) {
            clusterDetails.setFileSystemType(fileSystem.getType().name());
        }
    }

}
