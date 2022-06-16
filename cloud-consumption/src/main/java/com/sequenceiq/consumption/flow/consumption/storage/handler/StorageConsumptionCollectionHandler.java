package com.sequenceiq.consumption.flow.consumption.storage.handler;

import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerSelectors.STORAGE_CONSUMPTION_COLLECTION_HANDLER;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.SEND_CONSUMPTION_EVENT_EVENT;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import javax.inject.Inject;
import javax.validation.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageSizeRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageSizeResponse;
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
import com.sequenceiq.consumption.service.CredentialService;
import com.sequenceiq.consumption.service.EnvironmentService;
import com.sequenceiq.consumption.util.CloudStorageLocationUtil;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class StorageConsumptionCollectionHandler  extends AbstractStorageOperationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConsumptionCollectionHandler.class);

    private static final int DATE_RANGE_WIDTH_IN_DAYS = 2;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

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
            String cloudPlatform = detailedEnvironmentResponse.getCloudPlatform();
            if (cloudPlatform.equals(CloudPlatform.AWS.name())) {
                LOGGER.debug("Validating storage location '{}'.", consumption.getStorageLocation());
                CloudStorageLocationUtil.validateCloudStorageType(FileSystemType.S3, consumption.getStorageLocation());
                String bucketName = CloudStorageLocationUtil.getS3BucketName(consumption.getStorageLocation());

                LOGGER.debug("Getting credential for environment with CRN '{}'.", environmentCrn);
                Credential credential = credentialService.getCredentialByEnvCrn(environmentCrn);
                CloudCredential cloudCredential = credentialConverter.convert(credential);

                Date startTime = Date.from(Instant.now().minus(DATE_RANGE_WIDTH_IN_DAYS, ChronoUnit.DAYS));
                Date endTime = Date.from(Instant.now());
                String region = detailedEnvironmentResponse.getLocation().getName();

                ObjectStorageConnector connector = getObjectStorageConnector(cloudPlatform);
                ObjectStorageSizeRequest request = ObjectStorageSizeRequest.builder()
                        .withCredential(cloudCredential)
                        .withRegion(Region.region(region))
                        .withObjectStoragePath(bucketName)
                        .withStartTime(startTime)
                        .withEndTime(endTime)
                        .build();
                try {
                    LOGGER.debug("Getting storage size for bucket {} and timeframe from {} to {}", bucketName, startTime, endTime);
                    ObjectStorageSizeResponse response = connector.getObjectStorageSize(request);
                    return new SendStorageConsumptionEvent(SEND_CONSUMPTION_EVENT_EVENT.selector(), resourceId, resourceCrn,
                            new StorageConsumptionResult(response.getStorageInBytes()));
                } catch (CloudConnectorException e) {
                    LOGGER.error("Could not get storage size for bucket [{}]. Reason: {}", bucketName, e.getMessage(), e);
                    return new StorageConsumptionCollectionFailureEvent(resourceId, e, resourceCrn);
                }
            } else {
                String message = String.format("Storage consumption collection is not supported on cloud platform %s", cloudPlatform);
                LOGGER.error(message);
                return new StorageConsumptionCollectionFailureEvent(resourceId, new UnsupportedOperationException(message), resourceCrn);
            }
        } catch (ValidationException e) {
            LOGGER.error("Storage location validation failed. Reason: {}", e.getMessage(), e);
            return new StorageConsumptionCollectionFailureEvent(resourceId, e, resourceCrn);
        } catch (Exception e) {
            LOGGER.error("Storage consumption collection failed. Reason: {}", e.getMessage(), e);
            return new StorageConsumptionCollectionFailureEvent(resourceId, e, resourceCrn);
        }
    }

    private ObjectStorageConnector getObjectStorageConnector(String cloudPlatform) {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform));
        return Optional.ofNullable(cloudPlatformConnectors.get(cloudPlatformVariant))
                .map(CloudConnector<Object>::objectStorage)
                .orElseThrow(() -> new NotFoundException("No object storage connector for cloud platform: " + cloudPlatform));
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
