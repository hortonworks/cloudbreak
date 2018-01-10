package com.sequenceiq.cloudbreak.converter.v2.cli;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@Component
public class ClusterToClusterV2RequestConverter extends AbstractConversionServiceAwareConverter<Cluster, ClusterV2Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterToClusterV2RequestConverter.class);

    @Override
    public ClusterV2Request convert(Cluster source) {
        ClusterV2Request clusterV2Request = new ClusterV2Request();
        clusterV2Request.setAmbariRequest(getConversionService().convert(source, AmbariV2Request.class));
        clusterV2Request.setEmailNeeded(source.getEmailNeeded());
        clusterV2Request.setEmailTo(source.getEmailTo());
        clusterV2Request.setExecutorType(null);
        clusterV2Request.setFileSystem(getConversionService().convert(source.getFileSystem(), FileSystemRequest.class));
        clusterV2Request.setLdapConfigName(source.getLdapConfig() == null ? null : source.getLdapConfig().getName());
        clusterV2Request.setName(source.getName());
        clusterV2Request.setRdsConfigIds(new HashSet<>());
        for (RDSConfig rdsConfig : source.getRdsConfigs()) {
            clusterV2Request.getRdsConfigIds().add(rdsConfig.getId());
        }
        return clusterV2Request;
    }

}
