package com.sequenceiq.cloudbreak.cloud.aws.common.kms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;

import software.amazon.awssdk.services.kms.model.DescribeKeyRequest;
import software.amazon.awssdk.services.kms.model.DescribeKeyResponse;
import software.amazon.awssdk.services.kms.model.KeyListEntry;
import software.amazon.awssdk.services.kms.model.KeyMetadata;
import software.amazon.awssdk.services.kms.model.ListKeysRequest;
import software.amazon.awssdk.services.kms.model.ListKeysResponse;
import software.amazon.awssdk.services.kms.model.ListResourceTagsRequest;
import software.amazon.awssdk.services.kms.model.ListResourceTagsResponse;
import software.amazon.awssdk.services.kms.model.Tag;

@Component
public class AmazonKmsUtil {

    @Inject
    private AwsPageCollector awsPageCollector;

    public Map<String, Object> extractKeyMetadataMap(KeyMetadata keyMetaData) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("awsAccountId", keyMetaData.awsAccountId());
        meta.put("creationDate", keyMetaData.creationDate());
        meta.put("enabled", keyMetaData.enabled());
        meta.put("expirationModel", keyMetaData.expirationModel());
        meta.put("keyManager", keyMetaData.keyManager());
        meta.put("keyState", keyMetaData.keyState());
        meta.put("keyUsage", keyMetaData.keyUsage());
        meta.put("origin", keyMetaData.origin());
        meta.put("validTo", keyMetaData.validTo());
        return meta;
    }

    public List<KeyListEntry> listKeysWithAllPages(AmazonKmsClient kmsClient) {
        ListKeysRequest listKeysRequest = ListKeysRequest.builder().build();
        List<KeyListEntry> kmsKeys = awsPageCollector.collectPages(kmsClient::listKeys, listKeysRequest,
                ListKeysResponse::keys,
                ListKeysResponse::nextMarker,
                (req, token) -> req.toBuilder().marker(token).build());
        return kmsKeys;
    }

    public List<Tag> listResourceTagsWithAllPages(AmazonKmsClient kmsClient, ListResourceTagsRequest listResourceTagsRequest) {
        List<Tag> kmsTags = awsPageCollector.collectPages(kmsClient::listResourceTags, listResourceTagsRequest,
                ListResourceTagsResponse::tags,
                ListResourceTagsResponse::nextMarker,
                (req, token) -> req.toBuilder().marker(token).build());
        return kmsTags;
    }

    public KeyMetadata getKeyMetadataByKeyId(AmazonKmsClient kmsClient, String keyId) {
        DescribeKeyRequest describeKeyRequest = DescribeKeyRequest.builder()
                .keyId(keyId)
                .build();
        DescribeKeyResponse describeKeyResponse = kmsClient.describeKey(describeKeyRequest);
        return describeKeyResponse.keyMetadata();
    }

}
