package com.sequenceiq.cloudbreak.cloud.aws.common.kms;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;

import software.amazon.awssdk.services.kms.model.DescribeKeyRequest;
import software.amazon.awssdk.services.kms.model.DescribeKeyResponse;
import software.amazon.awssdk.services.kms.model.ExpirationModelType;
import software.amazon.awssdk.services.kms.model.KeyListEntry;
import software.amazon.awssdk.services.kms.model.KeyManagerType;
import software.amazon.awssdk.services.kms.model.KeyMetadata;
import software.amazon.awssdk.services.kms.model.KeyState;
import software.amazon.awssdk.services.kms.model.KeyUsageType;
import software.amazon.awssdk.services.kms.model.ListKeysRequest;
import software.amazon.awssdk.services.kms.model.ListKeysResponse;
import software.amazon.awssdk.services.kms.model.ListResourceTagsRequest;
import software.amazon.awssdk.services.kms.model.ListResourceTagsResponse;
import software.amazon.awssdk.services.kms.model.OriginType;
import software.amazon.awssdk.services.kms.model.Tag;

@ExtendWith(MockitoExtension.class)
class AmazonKmsUtilTest {

    private static final String NEXT_MARKER = "nextMarker";

    private static final String TOKEN = "token";

    private static final String KEY_ID = "keyId";

    @Mock
    private AwsPageCollector awsPageCollector;

    @InjectMocks
    private AmazonKmsUtil underTest;

    @Mock
    private AmazonKmsClient kmsClient;

    @Captor
    private ArgumentCaptor<Function<Object, Object>> resultProviderCaptor;

    @Captor
    private ArgumentCaptor<Object> requestCaptor;

    @Captor
    private ArgumentCaptor<Function<Object, List<Object>>> listFromResponseGetterCaptor;

    @Captor
    private ArgumentCaptor<Function<Object, String>> tokenFromResponseGetterCaptor;

    @Captor
    private ArgumentCaptor<BiFunction<Object, String, Object>> tokenToRequestSetterCaptor;

    @Test
    void extractKeyMetadataMapTest() {
        KeyMetadata keyMetaData = KeyMetadata.builder()
                .awsAccountId("test-awsAccountId")
                .creationDate(Instant.EPOCH)
                .enabled(true)
                .expirationModel(ExpirationModelType.KEY_MATERIAL_DOES_NOT_EXPIRE)
                .keyManager(KeyManagerType.AWS)
                .keyState(KeyState.ENABLED)
                .keyUsage(KeyUsageType.ENCRYPT_DECRYPT)
                .origin(OriginType.AWS_KMS)
                .validTo(Instant.MAX)
                .build();

        Map<String, Object> result = underTest.extractKeyMetadataMap(keyMetaData);

        assertThat(result).isEqualTo(Map.ofEntries(
                entry("awsAccountId", "test-awsAccountId"),
                entry("creationDate", Instant.EPOCH),
                entry("enabled", true),
                entry("expirationModel", ExpirationModelType.KEY_MATERIAL_DOES_NOT_EXPIRE),
                entry("keyManager", KeyManagerType.AWS),
                entry("keyState", KeyState.ENABLED),
                entry("keyUsage", KeyUsageType.ENCRYPT_DECRYPT),
                entry("origin", OriginType.AWS_KMS),
                entry("validTo", Instant.MAX)));
    }

    @Test
    void listKeysWithAllPagesTest() {
        List<Object> kmsKeys = List.of();
        when(awsPageCollector.collectPages(any(), any(), any(), any(), any())).thenReturn(kmsKeys);

        ListKeysRequest listKeysRequest = ListKeysRequest.builder().build();
        KeyListEntry keyListEntry = KeyListEntry.builder().build();
        ListKeysResponse listKeysResponse = ListKeysResponse.builder()
                .keys(keyListEntry)
                .nextMarker(NEXT_MARKER)
                .build();
        when(kmsClient.listKeys(listKeysRequest)).thenReturn(listKeysResponse);

        List<KeyListEntry> result = underTest.listKeysWithAllPages(kmsClient);

        assertThat(result).isSameAs(kmsKeys);
        verify(awsPageCollector).collectPages(resultProviderCaptor.capture(), requestCaptor.capture(), listFromResponseGetterCaptor.capture(),
                tokenFromResponseGetterCaptor.capture(), tokenToRequestSetterCaptor.capture());

        assertThat(resultProviderCaptor.getValue().apply(listKeysRequest)).isSameAs(listKeysResponse);
        assertThat(requestCaptor.getValue()).isEqualTo(listKeysRequest);
        assertThat(listFromResponseGetterCaptor.getValue().apply(listKeysResponse)).isEqualTo(List.of(keyListEntry));
        assertThat(tokenFromResponseGetterCaptor.getValue().apply(listKeysResponse)).isEqualTo(NEXT_MARKER);
        Object requestWithToken = tokenToRequestSetterCaptor.getValue().apply(listKeysRequest, TOKEN);
        assertThat(requestWithToken).isInstanceOf(ListKeysRequest.class);
        assertThat(((ListKeysRequest) requestWithToken).marker()).isEqualTo(TOKEN);
    }

    @Test
    void listResourceTagsWithAllPagesTest() {
        List<Object> kmsTags = List.of();
        when(awsPageCollector.collectPages(any(), any(), any(), any(), any())).thenReturn(kmsTags);

        ListResourceTagsRequest listResourceTagsRequest = ListResourceTagsRequest.builder().build();
        Tag tag = Tag.builder().build();
        ListResourceTagsResponse listResourceTagsResponse = ListResourceTagsResponse.builder()
                .tags(tag)
                .nextMarker(NEXT_MARKER)
                .build();
        when(kmsClient.listResourceTags(listResourceTagsRequest)).thenReturn(listResourceTagsResponse);

        List<Tag> result = underTest.listResourceTagsWithAllPages(kmsClient, listResourceTagsRequest);

        assertThat(result).isSameAs(kmsTags);
        verify(awsPageCollector).collectPages(resultProviderCaptor.capture(), requestCaptor.capture(), listFromResponseGetterCaptor.capture(),
                tokenFromResponseGetterCaptor.capture(), tokenToRequestSetterCaptor.capture());

        assertThat(resultProviderCaptor.getValue().apply(listResourceTagsRequest)).isSameAs(listResourceTagsResponse);
        assertThat(requestCaptor.getValue()).isEqualTo(listResourceTagsRequest);
        assertThat(listFromResponseGetterCaptor.getValue().apply(listResourceTagsResponse)).isEqualTo(List.of(tag));
        assertThat(tokenFromResponseGetterCaptor.getValue().apply(listResourceTagsResponse)).isEqualTo(NEXT_MARKER);
        Object requestWithToken = tokenToRequestSetterCaptor.getValue().apply(listResourceTagsRequest, TOKEN);
        assertThat(requestWithToken).isInstanceOf(ListResourceTagsRequest.class);
        assertThat(((ListResourceTagsRequest) requestWithToken).marker()).isEqualTo(TOKEN);
    }

    @Test
    void getKeyMetadataByKeyIdTest() {
        ArgumentCaptor<DescribeKeyRequest> describeKeyRequestCaptor = ArgumentCaptor.forClass(DescribeKeyRequest.class);
        KeyMetadata keyMetadata = KeyMetadata.builder().build();
        DescribeKeyResponse describeKeyResponse = DescribeKeyResponse.builder()
                .keyMetadata(keyMetadata)
                .build();

        when(kmsClient.describeKey(any(DescribeKeyRequest.class))).thenReturn(describeKeyResponse);

        KeyMetadata result = underTest.getKeyMetadataByKeyId(kmsClient, KEY_ID);

        assertThat(result).isSameAs(keyMetadata);
        verify(kmsClient).describeKey(describeKeyRequestCaptor.capture());
        DescribeKeyRequest describeKeyRequest = describeKeyRequestCaptor.getValue();
        assertThat(describeKeyRequest.keyId()).isEqualTo(KEY_ID);
    }

}