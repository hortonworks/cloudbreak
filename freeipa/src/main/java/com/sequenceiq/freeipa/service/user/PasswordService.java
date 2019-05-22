package com.sequenceiq.freeipa.service.user;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordResponse;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordRequest;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordResult;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Service
public class PasswordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordService.class);

    @Inject
    private StackService stackService;

    @Inject
    private EventBus eventBus;

    public SetPasswordResponse setPassword(String accountId, String username, String password) {
        LOGGER.debug("setting password for user {} in account {}", username, accountId);

        List<Stack> stacks = stackService.getAllByAccountId(accountId);

        if (stacks.isEmpty()) {
            throw new IllegalArgumentException("No stacks found for accountId " + accountId);
        }

        List<SetPasswordRequest> requests = new ArrayList<>();
        for (Stack stack : stacks) {
            requests.add(triggerSetPassword(stack, stack.getEnvironment(), username, password));
        }

        List<String> success = new ArrayList<>();
        List<String> failure = new ArrayList<>();

        for (SetPasswordRequest request : requests) {
            try {
                waitSetPassword(request);
                success.add(request.getEnvironment());
            } catch (OperationException e) {
                LOGGER.debug("Failed to set password for user {} in environment {}", username, request.getEnvironment());
                failure.add(request.getEnvironment());
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while setting passwords for user {} in account {}", username, accountId);
                throw new OperationException(e);
            }
        }

        return new SetPasswordResponse(success, failure);
    }

    private SetPasswordRequest triggerSetPassword(Stack stack, String environment, String username, String password) {
        SetPasswordRequest request = new SetPasswordRequest(stack.getId(), environment, username, password);
        eventBus.notify(request.selector(), new Event<>(request));
        return request;
    }

    private void waitSetPassword(SetPasswordRequest request) throws InterruptedException {
        SetPasswordResult result = request.await();
        if (result.getStatus().equals(EventStatus.FAILED)) {
            throw new OperationException(result.getErrorDetails());
        }
    }
}