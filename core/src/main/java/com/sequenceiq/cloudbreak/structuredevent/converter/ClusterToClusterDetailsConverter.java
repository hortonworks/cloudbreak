package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;

@Component
public class ClusterToClusterDetailsConverter {

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    public ClusterDetails convert(Cluster source) {
        ClusterDetails clusterDetails = new ClusterDetails();
        clusterDetails.setId(source.getId());
        clusterDetails.setName(source.getName());
        clusterDetails.setDescription(source.getDescription());
        clusterDetails.setStatus(source.getStatus().toString());
        clusterDetails.setStatusReason(source.getStatusReason());
        convertGatewayProperties(clusterDetails, source.getGateway());
        convertFileSystemProperties(clusterDetails, source.getFileSystem());
        convertComponents(clusterDetails, source);
        addDatabaseInfo(clusterDetails, source);
        return clusterDetails;
    }

    private void addDatabaseInfo(ClusterDetails clusterDetails, Cluster source) {
        RDSConfig rdsConfig = rdsConfigService.findByClusterIdAndType(source.getId(), DatabaseType.AMBARI);
        if (rdsConfig == null || DatabaseVendor.EMBEDDED == rdsConfig.getDatabaseEngine()) {
            clusterDetails.setDatabaseType(DatabaseVendor.EMBEDDED.name());
            clusterDetails.setExternalDatabase(Boolean.FALSE);
        } else {
            clusterDetails.setDatabaseType(rdsConfig.getDatabaseEngine().name());
            clusterDetails.setExternalDatabase(Boolean.TRUE);
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

    private void convertComponents(ClusterDetails clusterDetails, Cluster cluster) {
        AmbariRepo ambariRepo = clusterComponentConfigProvider.getAmbariRepo(cluster.getId());
        if (ambariRepo != null) {
            clusterDetails.setAmbariVersion(ambariRepo.getVersion());
        }
        StackRepoDetails stackRepoDetails = clusterComponentConfigProvider.getHDPRepo(cluster.getId());
        if (stackRepoDetails != null) {
            clusterDetails.setClusterType(stackRepoDetails.getStack().get(StackRepoDetails.REPO_ID_TAG));
            Optional<String> version = Optional.ofNullable(stackRepoDetails.getStack().get(StackRepoDetails.REPOSITORY_VERSION));
            clusterDetails.setClusterVersion(version.orElse(stackRepoDetails.getHdpVersion()));
        }
    }
}
