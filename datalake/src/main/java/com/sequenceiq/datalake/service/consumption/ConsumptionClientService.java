package com.sequenceiq.datalake.service.consumption;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionRequest;
import com.sequenceiq.consumption.client.ConsumptionInternalCrnClient;

@Service
public class ConsumptionClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionClientService.class);

    private final ConsumptionInternalCrnClient consumptionInternalCrnClient;

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public ConsumptionClientService(ConsumptionInternalCrnClient consumptionInternalCrnClient,
            WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor) {
        this.consumptionInternalCrnClient = consumptionInternalCrnClient;
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
    }

    public void scheduleStorageConsumptionCollection(String accountId, StorageConsumptionRequest request, String userCrn) {
        try {
            LOGGER.info("Executing storage consumption collection scheduling: account '{}', user '{}' and request '{}'", accountId, userCrn, request);
            consumptionInternalCrnClient.withInternalCrn()
                    .consumptionEndpoint()
                    .scheduleStorageConsumptionCollection(accountId, request, userCrn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to schedule storage consumption collection for account '%s', user '%s' and request '%s' due to '%s'.",
                    accountId, userCrn, request, errorMessage), e);
            throw new ConsumptionOperationFailedException(errorMessage, e);
        }
    }

    public void unscheduleStorageConsumptionCollection(String accountId, String monitoredResourceCrn, String storageLocation, String initiatorUserCrn) {
        try {
            LOGGER.info("Executing storage consumption collection unscheduling: account '{}', user '{} , resource '{}' and storage location '{}'",
                    accountId, initiatorUserCrn, monitoredResourceCrn, storageLocation);
            consumptionInternalCrnClient.withInternalCrn()
                    .consumptionEndpoint()
                    .unscheduleStorageConsumptionCollection(accountId, monitoredResourceCrn, storageLocation, initiatorUserCrn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(
                    String.format("Failed to unschedule storage consumption collection for account '%s', user '%s', resource '%s' and storage location " +
                            "'%s' due to '%s'.", accountId, initiatorUserCrn, monitoredResourceCrn, storageLocation, errorMessage), e);
            throw new ConsumptionOperationFailedException(errorMessage, e);
        }
    }

}
