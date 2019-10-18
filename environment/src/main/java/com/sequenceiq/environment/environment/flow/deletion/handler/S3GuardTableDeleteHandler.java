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
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.LocationAwareCredential;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent.EnvDeleteEventBuilder;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dao.domain.S3GuardTableCreation;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class S3GuardTableDeleteHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3GuardTableDeleteHandler.class);

    private final EnvironmentService environmentService;

    private final CloudPlatformConnectors cloudPlatformConnectors;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    protected S3GuardTableDeleteHandler(EventSender eventSender, EnvironmentService environmentService,
            CloudPlatformConnectors cloudPlatformConnectors, CredentialToCloudCredentialConverter credentialToCloudCredentialConverter) {
        super(eventSender);
        this.environmentService = environmentService;
        this.cloudPlatformConnectors = cloudPlatformConnectors;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
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

            EnvDeleteEvent envDeleteEvent = getEnvDeleteEvent(environmentDto);
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvDeleteFailedEvent failedEvent = new EnvDeleteFailedEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn());
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
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
        NoSqlTableDeleteRequest request = NoSqlTableDeleteRequest.builder()
                .withCloudPlatform(cloudPlatform)
                .withCredential(cloudCredential)
                .withRegion(location)
                .withTableName(dynamoDbTablename)
                .build();
        NoSqlTableDeleteResponse response = noSqlConnector.deleteNoSqlTable(request);
        return response.getStatus();
    }

    private NoSqlConnector getNoSqlConnector(String cloudPlatform) {
        return cloudPlatformConnectors.get(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform)).noSql();
    }

    private EnvDeleteEvent getEnvDeleteEvent(EnvironmentDto environmentDto) {
        return EnvDeleteEventBuilder.anEnvDeleteEvent()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withSelector(START_CLUSTER_DEFINITION_CLEANUP_EVENT.selector())
                .build();
    }

    @Override
    public String selector() {
        return DELETE_S3GUARD_TABLE_EVENT.selector();
    }
}
