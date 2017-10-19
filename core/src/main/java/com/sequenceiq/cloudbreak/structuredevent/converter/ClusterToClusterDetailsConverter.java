package com.sequenceiq.cloudbreak.structuredevent.converter;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Gateway;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;

@Component
public class ClusterToClusterDetailsConverter extends AbstractConversionServiceAwareConverter<Cluster, ClusterDetails> {
    @Inject
    private ConversionService conversionService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

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
        return clusterDetails;
    }

    private void convertKerberosConfig(ClusterDetails clusterDetails, Cluster source) {
        Boolean secure = source.isSecure();
        clusterDetails.setSecure(secure);
        if (secure) {
            clusterDetails.setSecure(Boolean.TRUE);
            KerberosConfig kerberosConfig = source.getKerberosConfig();
            String kerberosType = "New MIT Kerberos";
            if (kerberosConfig != null) {
                if (StringUtils.isNoneEmpty(kerberosConfig.getKerberosUrl())) {
                    if (StringUtils.isNoneEmpty(kerberosConfig.getKerberosLdapUrl())) {
                        kerberosType = "Existing Active Directory";
                    } else {
                        kerberosType = "Existing MIT Kerberos";
                    }
                }
            }
            clusterDetails.setKerberosType(kerberosType);
        }
    }

    private void convertGatewayProperties(ClusterDetails clusterDetails, Gateway gateway) {
        if (gateway != null) {
            clusterDetails.setGatewayEnabled(gateway.getEnableGateway());
            clusterDetails.setGatewayType(gateway.getGatewayType().toString());
            clusterDetails.setSsoType(gateway.getSsoType().toString());
        } else {
            clusterDetails.setGatewayEnabled(false);
        }
    }

    private void convertFileSystemProperties(ClusterDetails clusterDetails, FileSystem fileSystem) {
        if (fileSystem != null) {
            clusterDetails.setFileSystemType(fileSystem.getType());
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
            clusterDetails.setClusterVersion(stackRepoDetails.getHdpVersion());
        }
        AmbariDatabase ambariDatabase = clusterComponentConfigProvider.getAmbariDatabase(cluster.getId());
        if (ambariDatabase != null) {
            clusterDetails.setExternalDatabase(!ambariDatabase.getVendor().equals(DatabaseVendor.EMBEDDED.value()));
            clusterDetails.setDatabaseType(ambariDatabase.getVendor());
        } else {
            clusterDetails.setExternalDatabase(Boolean.FALSE);
            clusterDetails.setDatabaseType(DatabaseVendor.EMBEDDED.value());
        }
    }
}
