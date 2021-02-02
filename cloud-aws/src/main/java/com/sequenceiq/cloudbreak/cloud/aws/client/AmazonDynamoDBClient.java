package com.sequenceiq.cloudbreak.cloud.aws.client;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;

public class AmazonDynamoDBClient extends AmazonClient {

    private final AmazonDynamoDB client;

    public AmazonDynamoDBClient(AmazonDynamoDB client) {
        this.client = client;
    }

    public DescribeTableResult describeTable(String tableName) {
        return client.describeTable(tableName);
    }

    public DeleteTableResult deleteTable(String tableName) {
        return client.deleteTable(tableName);
    }

    public ListTablesResult listTables(ListTablesRequest listTablesRequest) {
        return client.listTables(listTablesRequest);
    }
}
