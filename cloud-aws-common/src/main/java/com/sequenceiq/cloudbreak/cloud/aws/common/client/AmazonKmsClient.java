package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyResponse;
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest;
import software.amazon.awssdk.services.kms.model.DescribeKeyResponse;
import software.amazon.awssdk.services.kms.model.ListAliasesRequest;
import software.amazon.awssdk.services.kms.model.ListAliasesResponse;
import software.amazon.awssdk.services.kms.model.ListKeysRequest;
import software.amazon.awssdk.services.kms.model.ListKeysResponse;
import software.amazon.awssdk.services.kms.model.ListResourceTagsRequest;
import software.amazon.awssdk.services.kms.model.ListResourceTagsResponse;

public class AmazonKmsClient extends AmazonClient {

    private final KmsClient client;

    public AmazonKmsClient(KmsClient client) {
        this.client = client;
    }

    public ListKeysResponse listKeys(ListKeysRequest listKeysRequest) {
        return client.listKeys(listKeysRequest);
    }

    public ListAliasesResponse listAliases(ListAliasesRequest listAliasesRequest) {
        return client.listAliases(listAliasesRequest);
    }

    public ListResourceTagsResponse listResourceTags(ListResourceTagsRequest listResourceTagsRequest) {
        return client.listResourceTags(listResourceTagsRequest);
    }

    public DescribeKeyResponse describeKey(DescribeKeyRequest describeKeyRequest) {
        return client.describeKey(describeKeyRequest);
    }

    public CreateKeyResponse createKey(CreateKeyRequest createKeyRequest) {
        return client.createKey(createKeyRequest);
    }

}
