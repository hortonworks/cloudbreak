package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyResponse;
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest;
import software.amazon.awssdk.services.kms.model.DescribeKeyResponse;
import software.amazon.awssdk.services.kms.model.EnableKeyRotationRequest;
import software.amazon.awssdk.services.kms.model.EnableKeyRotationResponse;
import software.amazon.awssdk.services.kms.model.GetKeyPolicyRequest;
import software.amazon.awssdk.services.kms.model.GetKeyPolicyResponse;
import software.amazon.awssdk.services.kms.model.ListAliasesRequest;
import software.amazon.awssdk.services.kms.model.ListAliasesResponse;
import software.amazon.awssdk.services.kms.model.ListKeysRequest;
import software.amazon.awssdk.services.kms.model.ListKeysResponse;
import software.amazon.awssdk.services.kms.model.ListResourceTagsRequest;
import software.amazon.awssdk.services.kms.model.ListResourceTagsResponse;
import software.amazon.awssdk.services.kms.model.PutKeyPolicyRequest;
import software.amazon.awssdk.services.kms.model.PutKeyPolicyResponse;
import software.amazon.awssdk.services.kms.model.RotateKeyOnDemandRequest;
import software.amazon.awssdk.services.kms.model.RotateKeyOnDemandResponse;
import software.amazon.awssdk.services.kms.model.ScheduleKeyDeletionRequest;
import software.amazon.awssdk.services.kms.model.ScheduleKeyDeletionResponse;

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

    public GetKeyPolicyResponse getKeyPolicy(GetKeyPolicyRequest getKeyPolicyRequest) {
        return client.getKeyPolicy(getKeyPolicyRequest);
    }

    public PutKeyPolicyResponse putKeyPolicy(PutKeyPolicyRequest putKeyPolicyRequest) {
        return client.putKeyPolicy(putKeyPolicyRequest);
    }

    public ScheduleKeyDeletionResponse scheduleKeyDeletion(ScheduleKeyDeletionRequest scheduleKeyDeletionRequest) {
        return client.scheduleKeyDeletion(scheduleKeyDeletionRequest);
    }

    public EnableKeyRotationResponse enableKeyRotation(EnableKeyRotationRequest enableKeyRotationRequest) {
        return client.enableKeyRotation(enableKeyRotationRequest);
    }

    public RotateKeyOnDemandResponse rotateKeyOnDemand(RotateKeyOnDemandRequest rotateKeyOnDemandRequest) {
        return client.rotateKeyOnDemand(rotateKeyOnDemandRequest);
    }

}
