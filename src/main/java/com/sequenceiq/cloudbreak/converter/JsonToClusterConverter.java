package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Status;

@Component
public class JsonToClusterConverter extends AbstractConversionServiceAwareConverter<ClusterRequest, Cluster> {
    @Override
    public Cluster convert(ClusterRequest source) {
        Cluster cluster = new Cluster();
        cluster.setName(source.getName());
        cluster.setStatus(Status.REQUESTED);
        cluster.setDescription(source.getDescription());
        cluster.setEmailNeeded(source.getEmailNeeded());
        Boolean enableSecurity = source.getEnableSecurity();
        cluster.setSecure(enableSecurity == null ? false : enableSecurity);
        cluster.setKerberosMasterKey(source.getKerberosMasterKey());
        cluster.setKerberosAdmin(source.getKerberosAdmin());
        cluster.setKerberosPassword(source.getKerberosPassword());
        return cluster;
    }
}
