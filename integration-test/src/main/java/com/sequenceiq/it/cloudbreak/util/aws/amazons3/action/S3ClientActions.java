package com.sequenceiq.it.cloudbreak.util.aws.amazons3.action;

import static java.lang.String.format;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.aws.amazons3.client.S3Client;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;

@Component
public class S3ClientActions extends S3Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ClientActions.class);

    public void deleteNonVersionedBucket(String baseLocation) {
        String bucketName = getBucketName(baseLocation);
        String keyPrefix = getKeyPrefix(baseLocation);

        try (software.amazon.awssdk.services.s3.S3Client s3Client = buildS3Client()) {
            if (doesBucketExist(s3Client, bucketName)) {
                try {
                    ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                            .bucket(bucketName)
                            .prefix(keyPrefix)
                            .build();
                    ListObjectsV2Response objectsResponse = s3Client.listObjectsV2(listObjectsRequest);

                    do {
                        List<ObjectIdentifier> deletableObjects = Lists.newArrayList();
                        for (S3Object s3Object : objectsResponse.contents()) {
                            deletableObjects.add(ObjectIdentifier.builder().key(s3Object.key()).build());
                        }
                        DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                                .bucket(bucketName)
                                .delete(Delete.builder().objects(deletableObjects).build())
                                .build();
                        s3Client.deleteObjects(deleteObjectsRequest);
                        if (objectsResponse.isTruncated()) {
                            objectsResponse = s3Client.listObjectsV2(listObjectsRequest);
                        }
                    } while (objectsResponse.isTruncated());
                } catch (AwsServiceException e) {
                    LOGGER.error("Amazon S3 couldn't process the call. So it has been returned with error!", e);
                    throw new TestFailException("Amazon S3 couldn't process the call.", e);
                } catch (SdkClientException e) {
                    LOGGER.error("Amazon S3 response could not been parsed, because of error!", e);
                    throw new TestFailException("Amazon S3 response could not been parsed", e);
                }
            } else {
                LOGGER.error("Amazon S3 bucket is not present with name: {}", bucketName);
                throw new TestFailException("Amazon S3 bucket is not present with name: " + bucketName);
            }
        }
    }

    private boolean doesBucketExist(software.amazon.awssdk.services.s3.S3Client s3Client, String bucketName) {
        return s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build())
                .sdkHttpResponse()
                .isSuccessful();
    }

    public void listBucketSelectedObject(String baseLocation, String selectedObject, boolean zeroContent) {
        String bucketName = getBucketName(baseLocation);
        String keyPrefix = getKeyPrefix(baseLocation);
        List<S3Object> filteredObjectSummaries;

        if (StringUtils.isEmpty(selectedObject)) {
            selectedObject = "ranger";
        }

        Log.log(LOGGER, format(" Amazon S3 URI: %s", getBaseLocationUri(baseLocation)));
        Log.log(LOGGER, format(" Amazon S3 Bucket: %s", bucketName));
        Log.log(LOGGER, format(" Amazon S3 Key Prefix: %s", keyPrefix));
        Log.log(LOGGER, format(" Amazon S3 Object: %s", selectedObject));
        try (software.amazon.awssdk.services.s3.S3Client s3Client = buildS3Client()) {

            if (doesBucketExist(s3Client, bucketName)) {
                try {
                    ListObjectsV2Request.Builder listObjectsRequestBuilder = ListObjectsV2Request.builder()
                            .bucket(bucketName)
                            .prefix(keyPrefix);
                    ListObjectsV2Response objectListing = s3Client.listObjectsV2(listObjectsRequestBuilder.build());

                    do {
                        String finalSelectedObject = selectedObject;
                        filteredObjectSummaries = objectListing.contents().stream()
                                .filter(objectSummary -> objectSummary.key().contains(finalSelectedObject))
                                .collect(Collectors.toList());

                        if (objectListing.isTruncated()) {
                            objectListing = s3Client.listObjectsV2(listObjectsRequestBuilder.continuationToken(objectListing.nextContinuationToken()).build());
                        }
                    } while (filteredObjectSummaries.isEmpty() && objectListing.isTruncated());

                if (filteredObjectSummaries.isEmpty()) {
                    LOGGER.error("Amazon S3 object: {} has 0 sub-objects or it is not present!", selectedObject);
                    throw new TestFailException(format("Amazon S3 object: %s has 0 sub-objects or it is not present!", selectedObject));
                } else {
                    Log.log(LOGGER, format(" Amazon S3 object: %s contains %d sub-objects or present with occurences.",
                            selectedObject, filteredObjectSummaries.size()));
                }

                    for (S3Object objectSummary : filteredObjectSummaries.stream().limit(10).collect(Collectors.toList())) {
                        ResponseInputStream<GetObjectResponse> object = s3Client.getObject(GetObjectRequest.builder()
                                .bucket(bucketName).key(objectSummary.key()).build(), ResponseTransformer.toInputStream());

                        if (!zeroContent && object.response().contentLength() == 0) {
                            LOGGER.error("Amazon S3 path: {} has 0 bytes of content!", objectSummary.key());
                            throw new TestFailException(format("Amazon S3 path: %s has 0 bytes of content!", objectSummary.key()));
                        }
                        try {
                            object.abort();
                            object.close();
                        } catch (IOException e) {
                            LOGGER.error("Unable to close Amazon S3 object {}. It has been returned with error!", objectSummary.key(), e);
                        }
                    }
                } catch (AwsServiceException e) {
                    LOGGER.error("Amazon S3 couldn't process the call. So it has been returned with error!", e);
                    throw new TestFailException("Amazon S3 couldn't process the call.", e);
                } catch (SdkClientException e) {
                    LOGGER.error("Amazon S3 response could not been parsed, because of error!", e);
                    throw new TestFailException("Amazon S3 response could not been parsed", e);
                }
            } else {
                LOGGER.error("Amazon S3 bucket is NOT present with name: {}", bucketName);
                throw new TestFailException("Amazon S3 bucket is NOT present with name: " + bucketName);
            }
        }

    }

    public String getLoggingUrl(String baseLocation, String clusterLogPath) {
        if (StringUtils.isNotBlank((baseLocation))) {
            URI baseLocationUri = getBaseLocationUri(baseLocation);
            String bucketName = getBucketName(baseLocation);
            String logPath = baseLocationUri.getPath();
            String deploymentS3Console = "https://s3.console.aws.amazon.com/s3/buckets/";

            Log.log(LOGGER, format(" Amazon S3 URI: %s", baseLocationUri));
            Log.log(LOGGER, format(" Amazon S3 Bucket: %s", bucketName));
            Log.log(LOGGER, format(" Amazon S3 Log Path: %s", logPath));
            Log.log(LOGGER, format(" Amazon S3 Cluster Logs: %s", clusterLogPath));

            if (StringUtils.containsIgnoreCase(getAwsProperties().getRegion(), "us-gov")) {
                deploymentS3Console = "https://console.amazonaws-us-gov.com/s3/buckets/";
            }
            if (StringUtils.contains(getKeyPrefix(baseLocation), clusterLogPath)) {
                return format("%s%s?region=%s&prefix=%s/&showversions=false",
                        deploymentS3Console, bucketName, getAwsProperties().getRegion(), getKeyPrefix(baseLocation));
            } else {
                return format("%s%s?region=%s&prefix=%s%s/&showversions=false",
                        deploymentS3Console, bucketName, getAwsProperties().getRegion(), getKeyPrefix(baseLocation), clusterLogPath);
            }
        } else {
            return null;
        }
    }
}
