package com.sequenceiq.redbeams.service;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.RetryableFlowResponse;
import com.sequenceiq.flow.domain.RetryResponse;
import com.sequenceiq.flow.domain.RetryableFlow;
import com.sequenceiq.flow.service.FlowRetryService;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Service
public class RedBeamsRetryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedBeamsRetryService.class);

    @Inject
    private FlowRetryService flowRetryService;

    @Inject
    private DBStackService dbStackService;

    public FlowIdentifier retry(String databaseCrn) {
        DBStack dbStack = dbStackService.getByCrn(databaseCrn);
        MDCBuilder.buildMdcContext(dbStack);
        RetryResponse retry = flowRetryService.retry(dbStack.getId());
        return retry.getFlowIdentifier();
    }

    public List<RetryableFlowResponse> getRetryableFlows(String databaseCrn) {
        DBStack dbStack = dbStackService.getByCrn(databaseCrn);
        MDCBuilder.buildMdcContext(dbStack);
        return collectRetryableFlowResponses(dbStack);
    }

    private List<RetryableFlowResponse> collectRetryableFlowResponses(DBStack dbStack) {
        List<RetryableFlow> retryableFlows = flowRetryService.getRetryableFlows(dbStack.getId());
        List<RetryableFlowResponse> flowResponses = retryableFlows.stream()
                .map(flow -> RetryableFlowResponse.Builder.builder()
                        .setName(flow.getName())
                        .setFailDate(flow.getFailDate())
                        .build())
                .collect(Collectors.toList());
        LOGGER.debug("Retryable flows: {}", flowResponses);
        return flowResponses;
    }
}
