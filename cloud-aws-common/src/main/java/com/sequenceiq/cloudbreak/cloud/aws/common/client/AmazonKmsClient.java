package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DescribeKeyRequest;
import com.amazonaws.services.kms.model.DescribeKeyResult;
import com.amazonaws.services.kms.model.ListAliasesRequest;
import com.amazonaws.services.kms.model.ListAliasesResult;
import com.amazonaws.services.kms.model.ListKeysRequest;
import com.amazonaws.services.kms.model.ListKeysResult;

public class AmazonKmsClient extends AmazonClient {

    private final AWSKMS client;

    public AmazonKmsClient(AWSKMS client) {
        this.client = client;
    }

    public ListKeysResult listKeys(ListKeysRequest listKeysRequest) {
        return client.listKeys(listKeysRequest);
    }

    public ListAliasesResult listAliases(ListAliasesRequest listAliasesRequest) {
        return client.listAliases(listAliasesRequest);
    }

    public DescribeKeyResult describeKey(DescribeKeyRequest describeKeyRequest) {
        return client.describeKey(describeKeyRequest);
    }
}
