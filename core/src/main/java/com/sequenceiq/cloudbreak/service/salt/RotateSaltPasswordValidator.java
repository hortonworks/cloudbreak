package com.sequenceiq.cloudbreak.service.salt;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.SaltBootstrapVersionChecker;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class RotateSaltPasswordValidator {

    @Inject
    private SaltBootstrapVersionChecker saltBootstrapVersionChecker;

    @Inject
    private EntitlementService entitlementService;

    public void validateRotateSaltPassword(StackDto stack) {
        if (stack.getStatus().isStopped()) {
            throw new BadRequestException("Rotating SaltStack user password is not supported for stopped clusters");
        }
        if (!entitlementService.isSaltUserPasswordRotationEnabled(stack.getAccountId())) {
            throw new BadRequestException("Rotating SaltStack user password is not supported in your account");
        }
        if (stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata().isEmpty()) {
            throw new IllegalStateException("Rotating SaltStack user password is not supported when there are no available gateway instances");
        }
        if (!isChangeSaltuserPasswordSupported(stack) && stack.getNotTerminatedInstanceMetaData().stream().anyMatch(im -> !im.isRunning())) {
            // fallback implementation re-bootstraps all nodes, so they have to be running
            throw new IllegalStateException("Rotating SaltStack user password is only supported when all instances are running");
        }
    }

    public boolean isChangeSaltuserPasswordSupported(StackDto stack) {
        return stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata().stream()
                .map(InstanceMetadataView::getImage)
                .allMatch(image -> saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(image));
    }
}
