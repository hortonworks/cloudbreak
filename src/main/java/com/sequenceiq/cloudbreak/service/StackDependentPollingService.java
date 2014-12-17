package com.sequenceiq.cloudbreak.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.flow.StackDependentPollerObject;

@Component
public class StackDependentPollingService<T extends StackDependentPollerObject> extends PollingService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDependentPollingService.class);

    @Autowired
    private StackRepository stackRepository;

    @Override
    protected boolean exitPolling(T t) {
        try {
            Stack byId = stackRepository.findById(t.getStack().getId());
            if (byId == null || byId.getStatus().equals(Status.DELETE_IN_PROGRESS)) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            return true;
        }
    }
}