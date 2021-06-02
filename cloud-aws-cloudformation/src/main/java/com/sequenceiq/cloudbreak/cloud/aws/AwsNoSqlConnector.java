package com.sequenceiq.cloudbreak.cloud.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.sequenceiq.cloudbreak.cloud.NoSqlConnector;
import com.sequenceiq.cloudbreak.cloud.RegionAware;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonDynamoDBClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.base.RegionAndCredentialAwareRequestBase;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteResponse;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataResponse;

@Service
public class AwsNoSqlConnector implements NoSqlConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNoSqlConnector.class);

    private final LegacyAwsClient awsClient;

    public AwsNoSqlConnector(LegacyAwsClient awsClient) {
        this.awsClient = awsClient;
    }

    @Override
    public NoSqlTableMetadataResponse getNoSqlTableMetaData(NoSqlTableMetadataRequest request) {
        try {
            LOGGER.debug("Calling DynamoDB.describeTable('{}')", request.getTableName());
            AmazonDynamoDBClient dynamoDbClient = getAmazonDynamoDB(request);
            DescribeTableResult describeTableResult = dynamoDbClient.describeTable(request.getTableName());
            LOGGER.debug("Successfully called DynamoDB.describeTable('{}')", request.getTableName());
            return NoSqlTableMetadataResponse.builder()
                    .withStatus(ResponseStatus.OK)
                    .withId(describeTableResult.getTable().getTableArn())
                    .withTableStatus(describeTableResult.getTable().getTableStatus())
                    .build();
        } catch (ResourceNotFoundException e) {
            LOGGER.info("DynamoDB table not found '{}'", request.getTableName());
            return NoSqlTableMetadataResponse.builder()
                    .withStatus(ResponseStatus.RESOURCE_NOT_FOUND)
                    .build();
        } catch (AmazonDynamoDBException e) {
            LOGGER.error(String.format("DynamoDB exception on describeTAble '%s'", request.getTableName()), e);
            throw new CloudConnectorException(String.format("Cannot get metadata for NoSQL table %s. "
                    + "Provider error message: %s", request.getTableName(), e.getErrorMessage()), e);
        }
    }

    @Override
    public NoSqlTableDeleteResponse deleteNoSqlTable(NoSqlTableDeleteRequest request) {
        try {
            LOGGER.debug("Calling DynamoDB.deleteTable('{}')", request.getTableName());
            AmazonDynamoDBClient dynamoDbClient = getAmazonDynamoDB(request);
            DeleteTableResult deleteTableResult = dynamoDbClient.deleteTable(request.getTableName());
            LOGGER.debug("Successfully called DynamoDB.deleteTable('{}')", request.getTableName());
            TableDescription tableDescription = deleteTableResult.getTableDescription();
            return NoSqlTableDeleteResponse.builder()
                    .withStatus(ResponseStatus.OK)
                    .withId(tableDescription.getTableArn())
                    .withTableStatus(tableDescription.getTableStatus())
                    .build();
        } catch (ResourceNotFoundException e) {
            LOGGER.info("DynamoDB table not found '{}'", request.getTableName());
            return NoSqlTableDeleteResponse.builder()
                    .withStatus(ResponseStatus.RESOURCE_NOT_FOUND)
                    .build();
        } catch (AmazonDynamoDBException e) {
            LOGGER.error(String.format("DynamoDB exception on deleteTable '%s'", request.getTableName()), e);
            throw new CloudConnectorException(String.format("Cannot delete NoSQL table %s. "
                    + "Provider error message: %s", request.getTableName(), e.getErrorMessage()), e);
        }
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_DEFAULT_VARIANT;
    }

    private AmazonDynamoDBClient getAmazonDynamoDB(RegionAndCredentialAwareRequestBase request) {
        AwsCredentialView awsCredentialView = new AwsCredentialView(request.getCredential());
        return awsClient.createDynamoDbClient(awsCredentialView, getRegion(request));
    }

    private String getRegion(RegionAware regionAware) {
        return regionAware.getRegion();
    }
}
