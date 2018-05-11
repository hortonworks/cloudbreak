package com.sequenceiq.cloudbreak.converter.v2.cli;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@Component
public class ClusterToClusterV2RequestConverter extends AbstractConversionServiceAwareConverter<Cluster, ClusterV2Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterToClusterV2RequestConverter.class);

    @Override
    public ClusterV2Request convert(Cluster source) {
        ClusterV2Request clusterV2Request = new ClusterV2Request();
        clusterV2Request.setAmbari(getConversionService().convert(source, AmbariV2Request.class));
        clusterV2Request.setEmailNeeded(source.getEmailNeeded());
        clusterV2Request.setEmailTo(source.getEmailTo());
        clusterV2Request.setExecutorType(null);
        clusterV2Request.setFileSystem(getConversionService().convert(source.getFileSystem(), FileSystemRequest.class));
        clusterV2Request.setLdapConfigName(source.getLdapConfig() == null ? null : source.getLdapConfig().getName());
        clusterV2Request.setName(source.getName());
        if (source.getRdsConfigs() != null && !source.getRdsConfigs().isEmpty()) {
            Set<String> rdsConfigNames = source.getRdsConfigs().stream()
                    .filter(rdsConfig -> rdsConfig.getStatus() == ResourceStatus.USER_MANAGED)
                    .map(RDSConfig::getName)
                    .collect(Collectors.toSet());
            clusterV2Request.setRdsConfigNames(Collections.unmodifiableSet(rdsConfigNames));
        }

        if (source.getProxyConfig() != null) {
            clusterV2Request.setProxyName(source.getProxyConfig().getName());
        }

        return clusterV2Request;
    }

}
