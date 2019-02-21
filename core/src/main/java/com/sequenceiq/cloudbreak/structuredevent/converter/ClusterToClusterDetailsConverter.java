package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;

@Component
public class ClusterToClusterDetailsConverter extends AbstractConversionServiceAwareConverter<Cluster, ClusterDetails> {

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private RdsConfigService rdsConfigService;

    @Override
    public ClusterDetails convert(Cluster source) {
        ClusterDetails clusterDetails = new ClusterDetails();
        clusterDetails.setId(source.getId());
        clusterDetails.setName(source.getName());
        clusterDetails.setDescription(source.getDescription());
        clusterDetails.setStatus(source.getStatus().toString());
        clusterDetails.setStatusReason(source.getStatusReason());
        convertKerberosConfig(clusterDetails, source);
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

    private void convertKerberosConfig(ClusterDetails clusterDetails, Cluster source) {
        if (source.getKerberosConfig() != null) {
            KerberosConfig kerberosConfig = source.getKerberosConfig();
            String kerberosType = "New MIT Kerberos";
            if (kerberosConfig != null) {
                if (StringUtils.isNoneEmpty(kerberosConfig.getUrl())) {
                    kerberosType = StringUtils.isNoneEmpty(kerberosConfig.getLdapUrl()) ? "Active Directory" : "MIT Kerberos";
                }
            }
            clusterDetails.setKerberosType(kerberosType);
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
        if (fileSystem != null) {
            clusterDetails.setFileSystemType(fileSystem.getType().name());
            clusterDetails.setDefaultFileSystem(fileSystem.isDefaultFs());
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
