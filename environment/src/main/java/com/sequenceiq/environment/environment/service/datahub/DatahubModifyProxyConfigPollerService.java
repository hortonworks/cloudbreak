package com.sequenceiq.environment.environment.service.datahub;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class DatahubModifyProxyConfigPollerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubModifyProxyConfigPollerService.class);

    @Value("${env.modifyproxy.datahub.polling.attempt:45}")
    private Integer attempt;

    @Value("${env.modifyproxy.datahub.polling.sleep.time:20}")
    private Integer sleeptime;

    private final DatahubService datahubService;

    private final DatahubPollerProvider datahubPollerProvider;

    private final MultipleFlowsResultEvaluator multipleFlowsResultEvaluator;

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public DatahubModifyProxyConfigPollerService(
            DatahubService datahubService,
            DatahubPollerProvider datahubPollerProvider,
            MultipleFlowsResultEvaluator multipleFlowsResultEvaluator,
            WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor) {
        this.datahubService = datahubService;
        this.datahubPollerProvider = datahubPollerProvider;
        this.multipleFlowsResultEvaluator = multipleFlowsResultEvaluator;
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
    }

    public void modifyProxyOnAttachedDatahubs(Long envId, String environmentCrn, String previousProxyConfigCrn) {
        Collection<StackViewV4Response> datahubs = datahubService.list(environmentCrn).getResponses();
        List<FlowIdentifier> flowIdentifiers = datahubs.stream()
                .map(datahub -> startModifyProxy(datahub.getCrn(), previousProxyConfigCrn))
                .collect(Collectors.toList());
        try {
            Polling.stopAfterAttempt(attempt)
                    .stopIfException(true)
                    .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                    .run(datahubPollerProvider.multipleFlowsPoller(envId, flowIdentifiers));
        } catch (PollerStoppedException e) {
            LOGGER.info("Data Hubs modify proxy config timed out or error happened.", e);
            throw new DatahubOperationFailedException("Data Hub modify proxy config timed out or error happened: " + e.getMessage());
        }
        if (multipleFlowsResultEvaluator.anyFailed(flowIdentifiers)) {
            throw new DatahubOperationFailedException("Data Hub modify proxy config error happened. One or more Data Hubs are not modified.");
        }
    }

    private FlowIdentifier startModifyProxy(String datahubCrn, String previousProxyConfigCrn) {
        return datahubService.modifyProxy(datahubCrn, previousProxyConfigCrn);
    }
}
