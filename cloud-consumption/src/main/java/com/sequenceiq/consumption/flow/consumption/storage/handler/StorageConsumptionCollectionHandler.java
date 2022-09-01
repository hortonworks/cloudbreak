package com.sequenceiq.consumption.flow.consumption.storage.handler;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerSelectors.STORAGE_CONSUMPTION_COLLECTION_HANDLER;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.SEND_CONSUMPTION_EVENT_EVENT;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.ConsumptionCalculator;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudConsumption;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeRequest;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeResponse;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.consumption.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.Credential;
import com.sequenceiq.consumption.dto.StorageConsumptionResult;
import com.sequenceiq.consumption.flow.consumption.storage.event.SendStorageConsumptionEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionFailureEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerEvent;
import com.sequenceiq.consumption.service.CredentialService;
import com.sequenceiq.consumption.service.EnvironmentService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class StorageConsumptionCollectionHandler  extends AbstractStorageOperationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConsumptionCollectionHandler.class);

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Selectable executeOperation(HandlerEvent<StorageConsumptionCollectionHandlerEvent> event) throws Exception {
        StorageConsumptionCollectionHandlerEvent consumptionEvent = event.getData();
        Consumption consumption = consumptionEvent.getContext().getConsumption();
        Long resourceId = consumptionEvent.getResourceId();
        String resourceCrn = consumptionEvent.getResourceCrn();
        String environmentCrn = consumption.getEnvironmentCrn();

        LOGGER.debug("Storage consumption collection started. resourceCrn: '{}'", resourceCrn);
        try {
            Credential credential = credentialService.getCredentialByEnvCrn(environmentCrn);
            CloudCredential cloudCredential = credentialConverter.convert(credential);
            DetailedEnvironmentResponse detailedEnvironmentResponse = environmentService.getByCrn(environmentCrn);
            LOGGER.debug("Getting credential for environment with CRN '{}'.", environmentCrn);
            String cloudPlatform = consumption.getConsumptionType().getStorageService().cloudPlatformName();
            CloudConsumption cloudConsumption = CloudConsumption.builder()
                    .withCloudCredential(cloudCredential)
                    .withStorageLocation(consumption.getStorageLocation())
                    .withRegion(detailedEnvironmentResponse.getLocation().getName())
                    .build();

            Optional<ConsumptionCalculator> consumptionCalculator = cloudPlatformConnectors
                    .getDefault(platform(cloudPlatform))
                    .consumptionCalculator(consumption.getConsumptionType().getStorageService());

            if (consumptionCalculator.isPresent()) {
                consumptionCalculator.get().validate(cloudConsumption);
                Date startTime = Date.from(Instant.now().minus(consumptionCalculator.get().dateRangeInDays(), ChronoUnit.DAYS));
                Date endTime = Date.from(Instant.now());
                StorageSizeRequest request = StorageSizeRequest.builder()
                        .withCredential(cloudConsumption.getCloudCredential())
                        .withRegion(Region.region(cloudConsumption.getRegion()))
                        .withObjectStoragePath(consumptionCalculator.get().getObjectId(consumption.getStorageLocation()))
                        .withStartTime(startTime)
                        .withEndTime(endTime)
                        .build();

                StorageSizeResponse response = consumptionCalculator.get().calculate(request);
                StorageConsumptionResult storageConsumptionResult = new StorageConsumptionResult(response.getStorageInBytes());
                return new SendStorageConsumptionEvent(SEND_CONSUMPTION_EVENT_EVENT.selector(),
                        resourceId,
                        resourceCrn,
                        storageConsumptionResult);
            } else {
                String message = String.format("Storage consumption collection is not supported on cloud platform %s", cloudPlatform);
                LOGGER.error(message);
                return new StorageConsumptionCollectionFailureEvent(resourceId, new UnsupportedOperationException(message), resourceCrn);
            }
        } catch (Exception e) {
            LOGGER.error("Storage consumption collection failed. Reason: {}", e.getMessage(), e);
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
