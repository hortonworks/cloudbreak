package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;
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
        AmbariStackDetailsJson stackDetailsJson = source.getAmbariStackDetails();
        if (stackDetailsJson != null) {
            AmbariStackDetails stackDetails = new AmbariStackDetails();
            stackDetails.setBaseURL(stackDetailsJson.getBaseURL());
            stackDetails.setOs(stackDetailsJson.getOs());
            stackDetails.setRepoId(stackDetailsJson.getRepoId());
            stackDetails.setStack(stackDetailsJson.getStack());
            stackDetails.setVersion(stackDetailsJson.getVersion());
            stackDetails.setVerify(stackDetailsJson.getVerify() == null ? false : stackDetailsJson.getVerify());
            cluster.setAmbariStackDetails(stackDetails);
        }
        return cluster;
    }
}
