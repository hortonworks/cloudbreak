package com.sequenceiq.cloudbreak.service.consumption;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.consumption.api.v1.consumption.model.request.CloudResourceConsumptionRequest;
import com.sequenceiq.consumption.client.ConsumptionInternalCrnClient;

@Service
public class ConsumptionClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionClientService.class);

    @Inject
    private ConsumptionInternalCrnClient consumptionInternalCrnClient;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public void scheduleCloudResourceConsumptionCollection(String accountId, CloudResourceConsumptionRequest request, String initiatorUserCrn) {
        try {
            LOGGER.info("Executing cloud resource consumption collection scheduling: account '{}', user '{}' and request '{}'", accountId, initiatorUserCrn,
                    request);
            consumptionInternalCrnClient.withInternalCrn()
                    .consumptionEndpoint()
                    .scheduleCloudResourceConsumptionCollection(accountId, request, initiatorUserCrn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to schedule cloud resource consumption collection for account '%s', user '%s' and request '%s' due to '%s'.",
                    accountId, initiatorUserCrn, request, errorMessage), e);
            throw new ConsumptionOperationFailedException(errorMessage, e);
        }
    }

    public void unscheduleCloudResourceConsumptionCollection(String accountId, String monitoredResourceCrn, String cloudResourceId, String initiatorUserCrn) {
        try {
            LOGGER.info("Executing cloud resource consumption collection unscheduling: account '{}', user '{}', CDP resource '{}' and cloud resource '{}'",
                    accountId, initiatorUserCrn, monitoredResourceCrn, cloudResourceId);
            consumptionInternalCrnClient.withInternalCrn()
                    .consumptionEndpoint()
                    .unscheduleCloudResourceConsumptionCollection(accountId, monitoredResourceCrn, cloudResourceId, initiatorUserCrn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(
                    String.format("Failed to unschedule cloud resource consumption collection for account '%s', user '%s', CDP resource '%s' and " +
                            "cloud resource '%s' due to '%s'.", accountId, initiatorUserCrn, monitoredResourceCrn, cloudResourceId, errorMessage), e);
            throw new ConsumptionOperationFailedException(errorMessage, e);
        }
    }

}
