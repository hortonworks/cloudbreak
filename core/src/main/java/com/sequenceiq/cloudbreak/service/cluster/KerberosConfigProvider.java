package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;

@Service
public class KerberosConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosConfigProvider.class);

    public void setKerberosConfigForWorkloadCluster(Cluster cluster, DatalakeResources datalakeResource) {
        Objects.requireNonNull(datalakeResource, "DatalakResource can not be null");

        if (datalakeResource.getKerberosConfig() != null) {
            cluster.setKerberosConfig(datalakeResource.getKerberosConfig());
            LOGGER.debug("Inherit Kerberos config from Data Lake cluster for stack {}, datalake stack {}", cluster.getName(), datalakeResource.getName());
        } else {
            LOGGER.debug("Datalake cluster doesn't have Kerberos config, cannot inherit, for stack {}, datalake stack {}",
                    cluster.getName(), datalakeResource.getName());
        }
    }
}