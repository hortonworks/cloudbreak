package com.sequenceiq.cloudbreak.service.freeipa;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Service
public class FreeipaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaService.class);

    @Inject
    private FreeipaClientService freeipaClientService;

    public boolean freeipaStatusInDesiredState(Stack stack, Set<Status> desiredStatuses) {
        boolean ret = false;
        if (stack.getEnvironmentCrn() != null) {
            try {
                DescribeFreeIpaResponse freeIpaResponse = freeipaClientService.getByEnvironmentCrn(stack.getEnvironmentCrn());
                ret = desiredStatuses.contains(freeIpaResponse.getStatus());
            } catch (CloudbreakServiceException e) {
                if (e.getCause() instanceof NotFoundException) {
                    LOGGER.debug("Cannot check the freeipa status: {}", e.getMessage(), e);
                    ret = true;
                } else {
                    LOGGER.debug("Unkonwn error during freeipa status check", e);
                }
            }
        }
        return ret;
    }
}
