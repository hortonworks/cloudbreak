package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Service
public class KerberosConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterService.class);

    void setKerberosConfigForWorkloadCluster(Cluster cluster, Stack datalake) {
        Stack stack = cluster.getStack();
        String stackName = stack.getName();
        if (stack.getDatalakeId() != null) {
            long start = System.currentTimeMillis();

            Cluster datalakeCluster = datalake.getCluster();
            if (Objects.nonNull(datalake) && Objects.nonNull(datalakeCluster)) {
                if (datalakeCluster.getKerberosConfig() != null) {
                    cluster.setSecure(Boolean.TRUE);
                    cluster.setKerberosConfig(datalakeCluster.getKerberosConfig());
                    LOGGER.info("Inherit Kerberos config from Data Lake cluster took {} ms for stack {}, datalake stack {}",
                            System.currentTimeMillis() - start, stackName, datalake.getName());
                } else {
                    cluster.setSecure(Boolean.FALSE);
                    LOGGER.info("Datalake cluster doesn't have Kerberos config, cannot inherit, "
                                    + "examine this took {} ms for stack {}, datalake stack {}",
                            System.currentTimeMillis() - start, stackName, datalake.getName());
                }
            } else {
                throw new BadRequestException(String.format("Datalake cluster does not exist for attached cluster %s", stackName));
            }
        }
    }
}