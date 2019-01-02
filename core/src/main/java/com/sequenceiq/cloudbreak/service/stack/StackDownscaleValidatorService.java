package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.GATEWAY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.GATEWAY_PRIMARY;

import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Component
public class StackDownscaleValidatorService {

    public void checkInstanceIsTheAmbariServerOrNot(String instancePublicIp, InstanceMetadataType metadataType) {
        if (GATEWAY.equals(metadataType) || GATEWAY_PRIMARY.equals(metadataType)) {
            throw new BadRequestException(String.format("Downscale for node [public IP: %s] is prohibited because it serves as a host the Ambari server",
                    instancePublicIp));
        }
    }

    public void checkUserHasRightToTerminateInstance(String owner, String userId, Long stackId) {
        if (!Objects.equals(owner, userId)) {
            throw new AccessDeniedException(String.format("Private stack (%s) is only modifiable by the owner.", stackId));
        }
    }

    public void checkClusterInValidStatus(Cluster cluster) {
        if (cluster.getStatus() == Status.STOPPED) {
            throw new BadRequestException("Cluster is in Stopped status. Please start the cluster for downscale.");
        }
    }

}
