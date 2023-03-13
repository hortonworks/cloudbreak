package com.sequenceiq.cloudbreak.cloud.aws.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.NoSqlConnector;
import com.sequenceiq.cloudbreak.cloud.RegionAware;
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

import software.amazon.awssdk.services.dynamodb.model.DeleteTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

@Service
public class AwsNoSqlConnector implements NoSqlConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNoSqlConnector.class);

    private final CommonAwsClient awsClient;

    public AwsNoSqlConnector(CommonAwsClient awsClient) {
        this.awsClient = awsClient;
    }

    @Override
    public NoSqlTableMetadataResponse getNoSqlTableMetaData(NoSqlTableMetadataRequest request) {
        try {
            LOGGER.debug("Calling DynamoDB.describeTable('{}')", request.getTableName());
            AmazonDynamoDBClient dynamoDbClient = getAmazonDynamoDB(request);
            DescribeTableResponse describeTableResponse = dynamoDbClient.describeTable(request.getTableName());
            LOGGER.debug("Successfully called DynamoDB.describeTable('{}')", request.getTableName());
            return NoSqlTableMetadataResponse.builder()
                    .withStatus(ResponseStatus.OK)
                    .withId(describeTableResponse.table().tableArn())
                    .withTableStatus(describeTableResponse.table().tableStatus().toString())
                    .build();
        } catch (ResourceNotFoundException e) {
            LOGGER.info("DynamoDB table not found '{}'", request.getTableName());
            return NoSqlTableMetadataResponse.builder()
                    .withStatus(ResponseStatus.RESOURCE_NOT_FOUND)
                    .build();
        } catch (DynamoDbException e) {
            LOGGER.error(String.format("DynamoDB exception on describeTAble '%s'", request.getTableName()), e);
            throw new CloudConnectorException(String.format("Cannot get metadata for NoSQL table %s. "
                    + "Provider error message: %s", request.getTableName(), e.awsErrorDetails().errorMessage()), e);
        }
    }

    @Override
    public NoSqlTableDeleteResponse deleteNoSqlTable(NoSqlTableDeleteRequest request) {
        try {
            LOGGER.debug("Calling DynamoDB.deleteTable('{}')", request.getTableName());
            AmazonDynamoDBClient dynamoDbClient = getAmazonDynamoDB(request);
            DeleteTableResponse deleteTableResponse = dynamoDbClient.deleteTable(request.getTableName());
            LOGGER.debug("Successfully called DynamoDB.deleteTable('{}')", request.getTableName());
            TableDescription tableDescription = deleteTableResponse.tableDescription();
            return NoSqlTableDeleteResponse.builder()
                    .withStatus(ResponseStatus.OK)
                    .withId(tableDescription.tableArn())
                    .withTableStatus(tableDescription.tableStatus().toString())
                    .build();
        } catch (ResourceNotFoundException e) {
            LOGGER.info("DynamoDB table not found '{}'", request.getTableName());
            return NoSqlTableDeleteResponse.builder()
                    .withStatus(ResponseStatus.RESOURCE_NOT_FOUND)
                    .build();
        } catch (DynamoDbException e) {
            LOGGER.error(String.format("DynamoDB exception on deleteTable '%s'", request.getTableName()), e);
            throw new CloudConnectorException(String.format("Cannot delete NoSQL table %s. "
                    + "Provider error message: %s", request.getTableName(), e.awsErrorDetails().errorMessage()), e);
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
