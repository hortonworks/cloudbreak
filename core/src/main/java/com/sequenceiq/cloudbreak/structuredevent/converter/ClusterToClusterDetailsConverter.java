package com.sequenceiq.cloudbreak.structuredevent.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsDetails;

@Component
public class ClusterToClusterDetailsConverter {

    @Inject
    private ConverterUtil converterUtil;

    public ClusterDetails convert(Cluster source) {
        ClusterDetails clusterDetails = new ClusterDetails();
        clusterDetails.setId(source.getId());
        clusterDetails.setName(source.getName());
        clusterDetails.setDescription(source.getDescription());
        clusterDetails.setStatus(source.getStatus().toString());
        clusterDetails.setStatusReason(source.getStatusReason());
        clusterDetails.setCreationStarted(source.getCreationStarted());
        clusterDetails.setCreationFinished(source.getCreationFinished());
        clusterDetails.setUpSince(source.getUpSince());
        clusterDetails.setRazEnabled(source.isRangerRazEnabled());
        convertGatewayProperties(clusterDetails, source.getGateway());
        convertFileSystemProperties(clusterDetails, source.getFileSystem());
        addDatabaseInfo(clusterDetails, source);
        return clusterDetails;
    }

    private void addDatabaseInfo(ClusterDetails clusterDetails, Cluster source) {
        if (source.getRdsConfigs() != null && !source.getRdsConfigs().isEmpty()) {
            clusterDetails.setDatabases(converterUtil.convertAll(source.getRdsConfigs(), RdsDetails.class));
        }
    }

    private void convertGatewayProperties(ClusterDetails clusterDetails, Gateway gateway) {
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
