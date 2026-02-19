package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

public class ConcurrentParameterizedStateRunner extends ParameterizedStateRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentParameterizedStateRunner.class);

    public ConcurrentParameterizedStateRunner(SaltStateService saltStateService, Set<String> targetHostnames, String state,
            Map<String, Object> parameters) {
        super(saltStateService, targetHostnames, state, parameters);
    }

    @Override
    public String submit(SaltConnector saltConnector) throws SaltJobFailedException {
        HostList targets = new HostList(getTargetHostnames());
        try {
            ApplyResponse applyResponse = saltStateService().applyConcurrentState(saltConnector, state, targets, parameters);
            LOGGER.debug("Executing salt state: '{}'. Parameters: '{}'. Targets: '{}'. applyResponse: '{}'",
                    state, parameters, targets, applyResponse.getResult());
            return getJid(applyResponse);
        } catch (JsonProcessingException e) {
            throw new SaltJobFailedException("ConcurrentParameterizedStateRunner job failed.", e);
        }
    }

    @Override
    public String toString() {
        return "ConcurrentParameterizedStateRunner{" + super.toString() + ", state: " + this.state + "}'";
    }
}