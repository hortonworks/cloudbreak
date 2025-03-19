package com.sequenceiq.datalake.service.loadbalancer.dns;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.entity.SdxCluster;

@Service
public class UpdateLoadBalancerDNSService {
    @Inject
    private StackV4Endpoint stackV4Endpoint;

    public void performLoadBalancerDNSUpdateOnPEM(SdxCluster sdxCluster) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> stackV4Endpoint.updateLoadBalancerPEMDNS(0L, sdxCluster.getClusterName(), initiatorUserCrn)
        );
    }

    public void performLoadBalancerDNSUpdateOnIPA(SdxCluster sdxCluster) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> stackV4Endpoint.updateLoadBalancerIPADNS(0L, sdxCluster.getClusterName(), initiatorUserCrn)
        );
    }
}
