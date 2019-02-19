package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Component
public class ClusterToClusterV4RequestConverter extends AbstractConversionServiceAwareConverter<Cluster, ClusterV4Request> {

    @Override
    public ClusterV4Request convert(Cluster source) {
        ClusterV4Request clusterV2Request = new ClusterV4Request();
        clusterV2Request.setAmbari(getConversionService().convert(source, AmbariV4Request.class));
        clusterV2Request.setExecutorType(source.getExecutorType());
        if (source.getFileSystem() != null) {
            clusterV2Request.setCloudStorage(getConversionService().convert(source.getFileSystem(), CloudStorageV4Request.class));
        }
        clusterV2Request.setLdapName(source.getLdapConfig() == null ? null : source.getLdapConfig().getName());
        clusterV2Request.setName(source.getName());
        if (source.getRdsConfigs() != null && !source.getRdsConfigs().isEmpty()) {
            Set<String> databaseNames = source.getRdsConfigs().stream()
                    .filter(rdsConfig -> rdsConfig.getStatus() == ResourceStatus.USER_MANAGED)
                    .map(RDSConfig::getName)
                    .collect(Collectors.toSet());
            clusterV2Request.setDatabases(Collections.unmodifiableSet(databaseNames));
        }

        if (source.getKerberosConfig() != null) {
            clusterV2Request.setKerberosName(source.getKerberosConfig().getName());
        }

        if (source.getProxyConfig() != null) {
            clusterV2Request.setProxyName(source.getProxyConfig().getName());
        }

        if (source.getGateway() != null) {
            clusterV2Request.setGateway(getConversionService().convert(source.getGateway(), GatewayV4Request.class));
        }

        return clusterV2Request;
    }

}
