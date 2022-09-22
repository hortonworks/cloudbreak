package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;

public class AmazonDynamoDBClient extends AmazonClient {

    private final DynamoDbClient client;

    public AmazonDynamoDBClient(DynamoDbClient client) {
        this.client = client;
    }

    public DescribeTableResponse describeTable(String tableName) {
        return client.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
    }

    public DeleteTableResponse deleteTable(String tableName) {
        return client.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
    }

    public ListTablesResponse listTables(ListTablesRequest listTablesRequest) {
        return client.listTables(listTablesRequest);
    }
}
