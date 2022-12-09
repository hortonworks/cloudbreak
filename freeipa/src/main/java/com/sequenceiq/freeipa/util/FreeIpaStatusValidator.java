package com.sequenceiq.freeipa.util;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class FreeIpaStatusValidator {

    public void throwBadRequestIfFreeIpaIsUnreachable(Stack stack) {
        if (stack != null && stack.getStackStatus() != null && stack.getStackStatus().getStatus() != null
                && stack.getStackStatus().getStatus().isFreeIpaUnreachableStatus()) {
            throw new BadRequestException("FreeIPA is unreachable. Please fix FreeIPA connectivity before retrying.");
        }
    }
}
