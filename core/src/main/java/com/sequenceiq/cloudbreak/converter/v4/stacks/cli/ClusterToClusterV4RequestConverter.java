package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cli.cm.ClusterToClouderaManagerV4RequestConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;

@Component
public class ClusterToClusterV4RequestConverter extends AbstractConversionServiceAwareConverter<Cluster, ClusterV4Request> {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private ClusterToClouderaManagerV4RequestConverter clouderaManagerV4RequestConverter;

    @Override
    public ClusterV4Request convert(Cluster source) {
        ClusterV4Request clusterRequest = new ClusterV4Request();
        convertClusterManager(source, clusterRequest);
        clusterRequest.setBlueprintName(source.getBlueprint().getName());
        clusterRequest.setValidateBlueprint(null);
        clusterRequest.setExecutorType(source.getExecutorType());
        clusterRequest.setUserName("");
        clusterRequest.setPassword("");
        if (source.getFileSystem() != null) {
            clusterRequest.setCloudStorage(getConversionService().convert(source.getFileSystem(), CloudStorageV4Request.class));
        }
        clusterRequest.setLdapName(source.getLdapConfig() == null ? null : source.getLdapConfig().getName());
        clusterRequest.setName(source.getName());
        if (source.getRdsConfigs() != null && !source.getRdsConfigs().isEmpty()) {
            Set<String> databaseNames = source.getRdsConfigs().stream()
                    .filter(rdsConfig -> rdsConfig.getStatus() == ResourceStatus.USER_MANAGED)
                    .map(RDSConfig::getName)
                    .collect(Collectors.toSet());
            clusterRequest.setDatabases(Collections.unmodifiableSet(databaseNames));
        }

        if (source.getKerberosConfig() != null) {
            clusterRequest.setKerberosName(source.getKerberosConfig().getName());
        }

        if (source.getProxyConfig() != null) {
            clusterRequest.setProxyName(source.getProxyConfig().getName());
        }

        if (source.getGateway() != null) {
            clusterRequest.setGateway(getConversionService().convert(source.getGateway(), GatewayV4Request.class));
        }

        return clusterRequest;
    }

    private void convertClusterManager(Cluster cluster, ClusterV4Request clusterRequest) {
        Blueprint blueprint = cluster.getBlueprint();
        if (blueprintService.isAmbariBlueprint(blueprint)) {
            clusterRequest.setAmbari(getConversionService().convert(cluster, AmbariV4Request.class));
        } else {
            clusterRequest.setCm(clouderaManagerV4RequestConverter.convert(cluster));
        }
    }

}
