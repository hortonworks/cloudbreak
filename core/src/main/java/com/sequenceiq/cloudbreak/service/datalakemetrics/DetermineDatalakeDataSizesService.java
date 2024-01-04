package com.sequenceiq.cloudbreak.service.datalakemetrics;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Service
public class DetermineDatalakeDataSizesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DetermineDatalakeDataSizesService.class);

    @Inject
    private ReactorFlowManager flowManager;

    public void determineDatalakeDataSizes(Stack stack, String operationId) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Initiating determination of datalake data sizes for stack {} and operation ID {}", stack.getId(), operationId);
        flowManager.triggerDetermineDatalakeDataSizes(stack.getId(), operationId);
    }
}
