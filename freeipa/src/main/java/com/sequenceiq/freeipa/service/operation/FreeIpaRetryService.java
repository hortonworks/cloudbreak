package com.sequenceiq.freeipa.service.operation;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.RetryableFlowResponse;
import com.sequenceiq.flow.domain.RetryResponse;
import com.sequenceiq.flow.domain.RetryableFlow;
import com.sequenceiq.flow.service.FlowRetryService;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaRetryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRetryService.class);

    @Inject
    private FlowRetryService flowRetryService;

    @Inject
    private StackService stackService;

    public FlowIdentifier retry(String environmentCrn, String accountId) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        RetryResponse retry = flowRetryService.retry(stack.getId());
        return retry.getFlowIdentifier();
    }

    public List<RetryableFlowResponse> getRetryableFlows(String environmentCrn, String accountId) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        return collectRetryableFlowResponses(stack);
    }

    private List<RetryableFlowResponse> collectRetryableFlowResponses(Stack stack) {
        List<RetryableFlow> retryableFlows = flowRetryService.getRetryableFlows(stack.getId());
        List<RetryableFlowResponse> flowResponses = retryableFlows.stream()
                .map(flow -> RetryableFlowResponse.Builder.builder().setName(flow.getName()).setFailDate(flow.getFailDate()).build())
                .collect(Collectors.toList());
        LOGGER.debug("Retryable flows: {}", flowResponses);
        return flowResponses;
    }
}
