package com.sequenceiq.environment.environment.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.NoSqlConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataResponse;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.LocationAwareCredential;
import com.sequenceiq.environment.parameters.dao.domain.S3GuardTableCreation;

@Service
public class NoSqlTableCreationModeDeterminerService {

    private final CloudPlatformConnectors cloudPlatformConnectors;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    public NoSqlTableCreationModeDeterminerService(CloudPlatformConnectors cloudPlatformConnectors,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter) {
        this.cloudPlatformConnectors = cloudPlatformConnectors;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
    }

    public S3GuardTableCreation determineCreationMode(LocationAwareCredential locationAwareCredential, String dynamoDbTablename) {
        return isDynamoDbTableExist(locationAwareCredential, dynamoDbTablename) ? S3GuardTableCreation.USE_EXISTING : S3GuardTableCreation.CREATE_NEW;
    }

    private boolean isDynamoDbTableExist(LocationAwareCredential locationAwareCredential, String dynamoDbTablename) {
        NoSqlTableMetadataResponse response = getNoSqlTableMetaData(locationAwareCredential, dynamoDbTablename);
        return response.getStatus() == ResponseStatus.OK && !"DELETING".equals(response.getTableStatus());
    }

    private NoSqlTableMetadataResponse getNoSqlTableMetaData(LocationAwareCredential locationAwareCredential, String dynamoDbTablename) {
        Credential credential = locationAwareCredential.getCredential();
        String cloudPlatform = credential.getCloudPlatform();
        String location = locationAwareCredential.getLocation();
        NoSqlConnector noSqlConnector = getNoSqlConnector(cloudPlatform);
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        NoSqlTableMetadataRequest request = NoSqlTableMetadataRequest.builder()
                .withCloudPlatform(cloudPlatform)
                .withCredential(cloudCredential)
                .withRegion(location)
                .withTableName(dynamoDbTablename)
                .build();
        return noSqlConnector.getNoSqlTableMetaData(request);
    }

    private NoSqlConnector getNoSqlConnector(String cloudPlatform) {
        return cloudPlatformConnectors.get(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform)).noSql();
    }
}
