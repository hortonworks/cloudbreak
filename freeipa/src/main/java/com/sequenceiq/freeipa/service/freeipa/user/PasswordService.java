package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordResponse;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordRequest;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordResult;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class PasswordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordService.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager freeIpaFlowManager;

    @Inject
    private CrnService crnService;

    @Inject
    private GrpcUmsClient umsClient;

    public SetPasswordResponse setPassword(String accountId, String userCrn, String password, Set<String> envs) {
        String userId = getUserIdFromUserCrn(userCrn);

        LOGGER.debug("setting password for user {} in account {}", userCrn, accountId);

        List<Stack> stacks = stackService.getAllByAccountId(accountId);

        if (stacks.isEmpty()) {
            throw new IllegalArgumentException("No stacks found for accountId " + accountId);
        }

        // Filter based on provided envs
        boolean filterForEnvs = false;
        if (envs != null && envs.size() > 0) {
            filterForEnvs = true;
        }

        List<SetPasswordRequest> requests = new ArrayList<>();
        for (Stack stack : stacks) {

            if (filterForEnvs) {
                if (envs.contains(stack.getEnvironmentCrn())) {
                    LOGGER.debug("Env list provided, setting password for env :{}", stack.getEnvironmentCrn());
                    requests.add(triggerSetPassword(stack, stack.getEnvironmentCrn(), userId, password));
                }
            } else {
                requests.add(triggerSetPassword(stack, stack.getEnvironmentCrn(), userId, password));
            }
        }

        List<String> success = new ArrayList<>();
        Map<String, String> failure = new HashMap<>();

        for (SetPasswordRequest request : requests) {
            try {
                waitSetPassword(request);
                success.add(request.getEnvironment());
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while setting passwords for user {} in account {}", userCrn, accountId);
                throw new OperationException(e);
            } catch (Exception e) {
                LOGGER.debug("Failed to set password for user {} in environment {}", userCrn, request.getEnvironment(), e);
                failure.put(request.getEnvironment(), e.getLocalizedMessage());
            }
        }

        return new SetPasswordResponse(success, failure);
    }

    private String getUserIdFromUserCrn(String userCrn) {
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User user = umsClient.getUserDetails(userCrn, userCrn, Optional.empty());
        // TODO replace this code with workloadUsername in DISTX-184
        return user.getEmail().split("@")[0];
    }

    private SetPasswordRequest triggerSetPassword(Stack stack, String environment, String username, String password) {
        SetPasswordRequest request = new SetPasswordRequest(stack.getId(), environment, username, password);
        freeIpaFlowManager.notify(request);
        return request;
    }

    private void waitSetPassword(SetPasswordRequest request) throws InterruptedException {
        SetPasswordResult result = request.await();
        if (result.getStatus().equals(EventStatus.FAILED)) {
            throw new OperationException(result.getErrorDetails());
        }
    }
}