package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonDynamoDBClient;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteResponse;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataResponse;

@RunWith(MockitoJUnitRunner.class)
public class AwsNoSqlConnectorTest {

    public static final String ARN = "arn:";

    public static final String ACTIVE_STATUS = "ACTIVE";

    public static final String DELETING_STATUS = "DELETING";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private AmazonDynamoDBClient dynamoDb;

    @InjectMocks
    private AwsNoSqlConnector underTest;

    @Before
    public void setUp() throws Exception {
        when(awsClient.createDynamoDbClient(any(), any())).thenReturn(dynamoDb);
    }

    @Test
    public void getNoSqlTableMetaDataOk() {
        TableDescription tableDescription = new TableDescription().withTableArn(ARN).withTableStatus(ACTIVE_STATUS);
        DescribeTableResult describeResult = new DescribeTableResult().withTable(tableDescription);
        when(dynamoDb.describeTable(argThat((ArgumentMatcher<String>) argument -> true))).thenReturn(describeResult);
        NoSqlTableMetadataResponse result = underTest.getNoSqlTableMetaData(new NoSqlTableMetadataRequest());
        assertEquals(ARN, result.getId());
        assertEquals(ACTIVE_STATUS, result.getTableStatus());
        assertEquals(ResponseStatus.OK, result.getStatus());
    }

    @Test
    public void getNoSqlTableMetaDataResourceNotFound() {
        when(dynamoDb.describeTable(argThat((ArgumentMatcher<String>) argument -> true))).thenThrow(new ResourceNotFoundException("not found"));
        NoSqlTableMetadataResponse result = underTest.getNoSqlTableMetaData(new NoSqlTableMetadataRequest());
        assertNull(result.getId());
        assertNull(result.getTableStatus());
        assertEquals(ResponseStatus.RESOURCE_NOT_FOUND, result.getStatus());
    }

    @Test
    public void getNoSqlTableMetaDataAwsError() {
        when(dynamoDb.describeTable(argThat((ArgumentMatcher<String>) argument -> true))).thenThrow(new AmazonDynamoDBException("provider error"));
        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("provider error");
        underTest.getNoSqlTableMetaData(new NoSqlTableMetadataRequest());
    }

    @Test
    public void deleteNoSqlTable() {
        TableDescription tableDescription = new TableDescription().withTableArn(ARN).withTableStatus(DELETING_STATUS);
        DeleteTableResult deleteResult = new DeleteTableResult().withTableDescription(tableDescription);
        when(dynamoDb.deleteTable(argThat((ArgumentMatcher<String>) argument -> true))).thenReturn(deleteResult);
        NoSqlTableDeleteResponse result = underTest.deleteNoSqlTable(new NoSqlTableDeleteRequest());
        assertEquals(ARN, result.getId());
        assertEquals(DELETING_STATUS, result.getTableStatus());
        assertEquals(ResponseStatus.OK, result.getStatus());
    }

    @Test
    public void getNoSqlTableResourceNotFound() {
        when(dynamoDb.deleteTable(argThat((ArgumentMatcher<String>) argument -> true))).thenThrow(new ResourceNotFoundException("not found"));
        NoSqlTableDeleteResponse result = underTest.deleteNoSqlTable(new NoSqlTableDeleteRequest());
        assertNull(result.getId());
        assertNull(result.getTableStatus());
        assertEquals(ResponseStatus.RESOURCE_NOT_FOUND, result.getStatus());
    }

    @Test
    public void getNoSqlTableAwsError() {
        when(dynamoDb.deleteTable(argThat((ArgumentMatcher<String>) argument -> true))).thenThrow(new AmazonDynamoDBException("provider error"));
        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("provider error");
        underTest.deleteNoSqlTable(new NoSqlTableDeleteRequest());
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
