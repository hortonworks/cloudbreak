package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.GATEWAY_PRIMARY;

import java.util.Objects;

import jakarta.ws.rs.ForbiddenException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class StackDownscaleValidatorService {

    public void checkInstanceIsTheClusterManagerServerOrNot(String instancePublicIp, InstanceMetadataType metadataType) {
        if (GATEWAY_PRIMARY.equals(metadataType)) {
            String additionalIpPartForTemplate = "";
            String messageTemplate = "Downscale for the given node%s is prohibited because it " +
                    "serves as a host the Cluster Manager server";
            if (StringUtils.isNotEmpty(instancePublicIp)) {
                additionalIpPartForTemplate = " [public IP: " + instancePublicIp + "]";
            }
            throw new BadRequestException(String.format(messageTemplate, additionalIpPartForTemplate));
        }
    }

    public void checkUserHasRightToTerminateInstance(String owner, String userId, Long stackId) {
        if (!Objects.equals(owner, userId)) {
            throw new ForbiddenException(String.format("Private stack (%s) is only modifiable by the owner.", stackId));
        }
    }

    public void checkClusterInValidStatus(StackView stack) {
        if (stack.getStatus() == Status.STOPPED) {
            throw new BadRequestException("Cluster is in Stopped status. Please start the cluster for downscale.");
        }
    }

}
