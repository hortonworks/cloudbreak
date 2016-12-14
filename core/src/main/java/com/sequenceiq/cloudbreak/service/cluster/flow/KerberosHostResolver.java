package com.sequenceiq.cloudbreak.service.cluster.flow;

import org.springframework.stereotype.Service;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.Cluster;

@Service
public class KerberosHostResolver {

    public String resolveHostForKerberos(Cluster cluster, String gatewayHost) {
        return Strings.isNullOrEmpty(cluster.getKerberosConfig().getKerberosUrl()) ? gatewayHost : cluster.getKerberosConfig().getKerberosUrl();
    }
}
