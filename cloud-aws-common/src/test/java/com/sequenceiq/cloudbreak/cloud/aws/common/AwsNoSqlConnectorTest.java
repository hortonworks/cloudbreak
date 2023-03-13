package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonDynamoDBClient;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteResponse;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataResponse;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

@ExtendWith(MockitoExtension.class)
public class AwsNoSqlConnectorTest {

    public static final String ARN = "arn:";

    public static final String ACTIVE_STATUS = "ACTIVE";

    public static final String DELETING_STATUS = "DELETING";

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private AmazonDynamoDBClient dynamoDb;

    @InjectMocks
    private AwsNoSqlConnector underTest;

    @BeforeEach
    public void setUp() throws Exception {
        lenient().when(awsClient.createDynamoDbClient(any(), any())).thenReturn(dynamoDb);
    }

    @Test
    public void getNoSqlTableMetaDataOk() {
        TableDescription tableDescription = TableDescription.builder().tableArn(ARN).tableStatus(ACTIVE_STATUS).build();
        DescribeTableResponse describeResult = DescribeTableResponse.builder().table(tableDescription).build();
        when(dynamoDb.describeTable(argThat(argument -> true))).thenReturn(describeResult);
        NoSqlTableMetadataResponse result = underTest.getNoSqlTableMetaData(new NoSqlTableMetadataRequest());
        assertEquals(ARN, result.getId());
        assertEquals(ACTIVE_STATUS, result.getTableStatus());
        assertEquals(ResponseStatus.OK, result.getStatus());
    }

    @Test
    public void getNoSqlTableMetaDataResourceNotFound() {
        when(dynamoDb.describeTable(argThat(argument -> true)))
                .thenThrow(ResourceNotFoundException.builder().message("not found").build());
        NoSqlTableMetadataResponse result = underTest.getNoSqlTableMetaData(new NoSqlTableMetadataRequest());
        assertNull(result.getId());
        assertNull(result.getTableStatus());
        assertEquals(ResponseStatus.RESOURCE_NOT_FOUND, result.getStatus());
    }

    @Test
    public void getNoSqlTableMetaDataAwsError() {
        when(dynamoDb.describeTable(argThat(argument -> true)))
                .thenThrow(DynamoDbException.builder().awsErrorDetails(AwsErrorDetails.builder().errorMessage("provider error").build()).build());
        assertThrows(CloudConnectorException.class,
                () -> underTest.getNoSqlTableMetaData(new NoSqlTableMetadataRequest()),
                "Cannot get metadata for NoSQL table null. Provider error message: provider error");
    }

    @Test
    public void deleteNoSqlTable() {
        TableDescription tableDescription = TableDescription.builder().tableArn(ARN).tableStatus(DELETING_STATUS).build();
        DeleteTableResponse deleteResult = DeleteTableResponse.builder().tableDescription(tableDescription).build();
        when(dynamoDb.deleteTable(argThat(argument -> true))).thenReturn(deleteResult);
        NoSqlTableDeleteResponse result = underTest.deleteNoSqlTable(new NoSqlTableDeleteRequest());
        assertEquals(ARN, result.getId());
        assertEquals(DELETING_STATUS, result.getTableStatus());
        assertEquals(ResponseStatus.OK, result.getStatus());
    }

    @Test
    public void getNoSqlTableResourceNotFound() {
        when(dynamoDb.deleteTable(argThat(argument -> true)))
                .thenThrow(ResourceNotFoundException.builder().message("not found").build());
        NoSqlTableDeleteResponse result = underTest.deleteNoSqlTable(new NoSqlTableDeleteRequest());
        assertNull(result.getId());
        assertNull(result.getTableStatus());
        assertEquals(ResponseStatus.RESOURCE_NOT_FOUND, result.getStatus());
    }

    @Test
    public void getNoSqlTableAwsError() {
        when(dynamoDb.deleteTable(argThat(argument -> true)))
                .thenThrow(DynamoDbException.builder().awsErrorDetails(AwsErrorDetails.builder().errorMessage("provider error").build()).build());
        assertThrows(CloudConnectorException.class,
                () -> underTest.deleteNoSqlTable(new NoSqlTableDeleteRequest()),
                "Cannot delete NoSQL table null. Provider error message: provider error");
    }

    @Test
    public void platform() {
        assertEquals(AwsConstants.AWS_PLATFORM, underTest.platform());
    }

    @Test
    public void variant() {
        assertEquals(AwsConstants.AWS_DEFAULT_VARIANT, underTest.variant());
    }
}
