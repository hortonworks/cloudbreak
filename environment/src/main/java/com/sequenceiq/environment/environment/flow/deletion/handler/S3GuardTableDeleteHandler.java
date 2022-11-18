package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_S3GUARD_TABLE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_CLUSTER_DEFINITION_CLEANUP_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.NoSqlConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteResponse;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataResponse;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.LocationAwareCredential;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.parameter.dto.s3guard.S3GuardTableCreation;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class S3GuardTableDeleteHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3GuardTableDeleteHandler.class);

    private final EnvironmentService environmentService;

    private final HandlerExceptionProcessor exceptionProcessor;

    private final CloudPlatformConnectors cloudPlatformConnectors;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    protected S3GuardTableDeleteHandler(EventSender eventSender, EnvironmentService environmentService,
            CloudPlatformConnectors cloudPlatformConnectors, CredentialToCloudCredentialConverter credentialToCloudCredentialConverter,
            HandlerExceptionProcessor exceptionProcessor) {
        super(eventSender);
        this.environmentService = environmentService;
        this.exceptionProcessor = exceptionProcessor;
        this.cloudPlatformConnectors = cloudPlatformConnectors;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
    }

    @Override
    public void accept(Event<EnvironmentDeletionDto> environmentDtoEvent) {
        EnvironmentDeletionDto environmentDeletionDto = environmentDtoEvent.getData();
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        EnvDeleteEvent envDeleteEvent = getEnvDeleteEvent(environmentDeletionDto);

        try {
            environmentService.findEnvironmentById(environmentDto.getId()).ifPresent(environment -> {
                BaseParameters environmentParameters = environment.getParameters();
                if (environmentParameters instanceof AwsParameters) {
                    AwsParameters awsParameters = (AwsParameters) environmentParameters;
                    deleteS3GuardTable(environment, awsParameters);
                } else {
                    LOGGER.info("Environment parameters determine a non-AWS environment. DynamoDB table deletion is not necessary.");
                }
            });
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            exceptionProcessor.handle(new HandlerFailureConjoiner(e, environmentDtoEvent, envDeleteEvent), LOGGER, eventSender(), selector());
        }
    }

    private void deleteS3GuardTable(Environment environment, AwsParameters awsParameters) {
        if (S3GuardTableCreation.CREATE_NEW == awsParameters.getS3guardTableCreation()) {
            LocationAwareCredential locationAwareCredential = getLocationAwareCredential(environment);
            ResponseStatus responseStatus = deleteNoSqlTable(locationAwareCredential, awsParameters.getS3guardTableName());
            if (responseStatus == ResponseStatus.OK) {
                LOGGER.info("'{}' DynamoDB table deletion initiated at cloud provider.", awsParameters.getS3guardTableName());
            } else if (responseStatus == ResponseStatus.RESOURCE_NOT_FOUND) {
                LOGGER.warn("'{}' DynamoDB table deletion initiated but table was not found.", awsParameters.getS3guardTableName());
            }
        } else {
            LOGGER.info("'{}' DynamoDB table had already been existing before environment creation. Will not delete it.",  awsParameters.getS3guardTableName());
        }
    }

    private LocationAwareCredential getLocationAwareCredential(Environment environment) {
        return LocationAwareCredential.builder()
                .withCredential(environment.getCredential())
                .withLocation(environment.getLocation())
                .build();
    }

    private ResponseStatus deleteNoSqlTable(LocationAwareCredential locationAwareCredential, String dynamoDbTablename) {
        Credential credential = locationAwareCredential.getCredential();
        String cloudPlatform = credential.getCloudPlatform();
        String location = locationAwareCredential.getLocation();
        NoSqlConnector noSqlConnector = getNoSqlConnector(cloudPlatform);
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        NoSqlTableMetadataRequest noSqlTableMetadataRequest = NoSqlTableMetadataRequest.builder()
                .withCloudPlatform(cloudPlatform)
                .withCredential(cloudCredential)
                .withRegion(location)
                .withTableName(dynamoDbTablename)
                .build();
        NoSqlTableMetadataResponse noSqlTableMetaData = noSqlConnector.getNoSqlTableMetaData(noSqlTableMetadataRequest);
        if (ResponseStatus.OK.equals(noSqlTableMetaData.getStatus())) {
            NoSqlTableDeleteRequest request = NoSqlTableDeleteRequest.builder()
                    .withCloudPlatform(cloudPlatform)
                    .withCredential(cloudCredential)
                    .withRegion(location)
                    .withTableName(dynamoDbTablename)
                    .build();
            NoSqlTableDeleteResponse response = noSqlConnector.deleteNoSqlTable(request);
            return response.getStatus();
        } else {
            return ResponseStatus.OK;
        }
    }

    private NoSqlConnector getNoSqlConnector(String cloudPlatform) {
        return cloudPlatformConnectors.get(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform)).noSql();
    }

    private EnvDeleteEvent getEnvDeleteEvent(EnvironmentDeletionDto environmentDeletionDto) {
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();

        return EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withForceDelete(environmentDeletionDto.isForceDelete())
                .withSelector(START_CLUSTER_DEFINITION_CLEANUP_EVENT.selector())
                .build();
    }

    @Override
    public String selector() {
        return DELETE_S3GUARD_TABLE_EVENT.selector();
    }
}
