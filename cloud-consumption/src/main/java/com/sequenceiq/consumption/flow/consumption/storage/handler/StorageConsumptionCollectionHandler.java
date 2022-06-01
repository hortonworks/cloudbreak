package com.sequenceiq.consumption.flow.consumption.storage.handler;

import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerSelectors.STORAGE_CONSUMPTION_COLLECTION_HANDLER;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.SEND_CONSUMPTION_EVENT_EVENT;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.consumption.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.Credential;
import com.sequenceiq.consumption.dto.StorageConsumptionResult;
import com.sequenceiq.consumption.flow.consumption.storage.event.SendStorageConsumptionEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionFailureEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerEvent;
import com.sequenceiq.consumption.service.CloudWatchService;
import com.sequenceiq.consumption.service.CredentialService;
import com.sequenceiq.consumption.service.EnvironmentService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class StorageConsumptionCollectionHandler  extends AbstractStorageOperationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConsumptionCollectionHandler.class);

    private static final int AMOUNT_TO_SUBTRACT = 2;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CloudWatchService cloudWatchService;

    @Inject
    private EnvironmentService environmentService;

    @Override
    public Selectable executeOperation(HandlerEvent<StorageConsumptionCollectionHandlerEvent> event) throws Exception {
        StorageConsumptionCollectionHandlerEvent data = event.getData();

        Consumption consumption = data.getContext().getConsumption();
        String environmentCrn = consumption.getEnvironmentCrn();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        LOGGER.debug("Storage consumption collection started. resourceCrn: '{}'", resourceCrn);

        try {
            DetailedEnvironmentResponse detailedEnvironmentResponse = environmentService.getByCrn(environmentCrn);
            LOGGER.debug("Getting credential for environment with CRN [{}].", environmentCrn);
            if (detailedEnvironmentResponse.getCloudPlatform().equals(CloudPlatform.AWS.name())) {
                Credential credential = credentialService.getCredentialByEnvCrn(environmentCrn);
                CloudCredential cloudCredential = credentialConverter.convert(credential);
                Date startTime = Date.from(Instant.now().minus(AMOUNT_TO_SUBTRACT, ChronoUnit.DAYS));
                Date endTime = Date.from(Instant.now());
                String bucketName = consumption.getStorageLocation().replace(FileSystemType.S3.getProtocol() + "://", "").split("/")[0];
                LOGGER.debug("Getting the bucket size for the bucket {} from {} to {}", bucketName, startTime, endTime);

                String region = detailedEnvironmentResponse.getLocation().getName();
                GetMetricStatisticsResult result = cloudWatchService.getBucketSize(cloudCredential, region, startTime, endTime, bucketName);
                Optional<Datapoint> optionalDatapoint = result.getDatapoints().stream().max(Comparator.comparing(Datapoint::getTimestamp));
                if (optionalDatapoint.isPresent()) {
                    Datapoint datapoint = optionalDatapoint.get();
                    LOGGER.debug("The data point requested : {}", datapoint);
                    StorageConsumptionResult storageConSumptionResult = new StorageConsumptionResult(datapoint.getMaximum());
                    LOGGER.debug("Storage consumption collection started. resourceCrn: '{}'", resourceCrn);
                    return new SendStorageConsumptionEvent(SEND_CONSUMPTION_EVENT_EVENT.selector(), resourceId, resourceCrn, storageConSumptionResult);
                } else {
                    LOGGER.error("No storage size data point returned from CloudWatch");
                    throw new NotFoundException("No storage size data point returned from CloudWatch");
                }
            } else {
                LOGGER.error("Error collecting storage collection as Cloud platform not supported");
                throw new CloudOperationNotSupportedException(String.format("Storage consumption collection is not supported on %s",
                        detailedEnvironmentResponse.getCloudPlatform()));
            }
        } catch (Exception e) {
            return new StorageConsumptionCollectionFailureEvent(resourceId, e, resourceCrn);
        }
    }

    @Override
    public String getOperationName() {
        return "Collect storage consumption data";
    }

    @Override
    public String selector() {
        return STORAGE_CONSUMPTION_COLLECTION_HANDLER.selector();
    }
}
