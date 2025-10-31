package com.sequenceiq.cloudbreak.service.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.model.StatusKind;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.dto.StackDto;

@Component
public class UpdatePublicDnsEntriesInPemValidator {

    public static final ImmutableSet<Status> NON_TRIGGERABLE_FINAL_STATES = Sets.immutableEnumSet(
            Status.CREATE_FAILED,
            Status.STOPPED,
            Status.STOP_FAILED,
            Status.EXTERNAL_DATABASE_CREATION_FAILED,
            Status.DELETED_ON_PROVIDER_SIDE,
            Status.DELETE_FAILED,
            Status.DELETE_COMPLETED
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatePublicDnsEntriesInPemValidator.class);

    public void validate(StackDto stack) {
        boolean stackIsInNotTriggerableStatus = StatusKind.PROGRESS.equals(stack.getStatus().getStatusKind())
                || NON_TRIGGERABLE_FINAL_STATES.contains(stack.getStatus());
        if (stackIsInNotTriggerableStatus) {
            String message = String.format("The update of public DNS entries can't be performed when the cluster status is '%s", stack.getStatus());
            LOGGER.info(message);
            throw new BadRequestException(message);
        }
    }
}
