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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.Lists;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.aws.amazons3.client.S3Client;

@Component
public class S3ClientActions extends S3Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3ClientActions.class);

    public void deleteNonVersionedBucket(String baseLocation) {
        AmazonS3 s3Client = buildS3Client();
        String bucketName = getBucketName(baseLocation);
        String keyPrefix = getKeyPrefix(baseLocation);

        if (s3Client.doesBucketExistV2(bucketName)) {
            try {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                        .withBucketName(bucketName)
                        .withPrefix(keyPrefix);
                ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);

                do {
                    List<KeyVersion> deletableObjects = Lists.newArrayList();
                    for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                        deletableObjects.add(new KeyVersion(objectSummary.getKey()));
                    }
                    DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName);
                    deleteObjectsRequest.setKeys(deletableObjects);
                    s3Client.deleteObjects(deleteObjectsRequest);
                    if (objectListing.isTruncated()) {
                        objectListing = s3Client.listNextBatchOfObjects(objectListing);
                    }
                } while (objectListing.isTruncated());
            } catch (AmazonServiceException e) {
                LOGGER.error("Amazon S3 couldn't process the call. So it has been returned with error!", e);
                throw new TestFailException("Amazon S3 couldn't process the call.", e);
            } catch (SdkClientException e) {
                LOGGER.error("Amazon S3 response could not been parsed, because of error!", e);
                throw new TestFailException("Amazon S3 response could not been parsed", e);
            } finally {
                s3Client.shutdown();
            }
        } else {
            LOGGER.error("Amazon S3 bucket is not present with name: {}", bucketName);
            throw new TestFailException("Amazon S3 bucket is not present with name: " + bucketName);
        }
    }

    public void listBucketSelectedObject(String baseLocation, String selectedObject, boolean zeroContent) {
        AmazonS3 s3Client = buildS3Client();
        String bucketName = getBucketName(baseLocation);
        String keyPrefix = getKeyPrefix(baseLocation);
        List<S3ObjectSummary> filteredObjectSummaries;

        if (StringUtils.isEmpty(selectedObject)) {
            selectedObject = "ranger";
        }

        Log.log(LOGGER, format(" Amazon S3 URI: %s", getBaseLocationUri(baseLocation)));
        Log.log(LOGGER, format(" Amazon S3 Bucket: %s", bucketName));
        Log.log(LOGGER, format(" Amazon S3 Key Prefix: %s", keyPrefix));
        Log.log(LOGGER, format(" Amazon S3 Object: %s", selectedObject));

        if (s3Client.doesBucketExistV2(bucketName)) {
            try {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                        .withBucketName(bucketName)
                        .withPrefix(keyPrefix);
                ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);

                do {
                    String finalSelectedObject = selectedObject;
                    filteredObjectSummaries = objectListing.getObjectSummaries().stream()
                            .filter(objectSummary -> objectSummary.getKey().contains(finalSelectedObject))
                            .collect(Collectors.toList());

                    if (objectListing.isTruncated()) {
                        objectListing = s3Client.listNextBatchOfObjects(objectListing);
                    }
                } while (filteredObjectSummaries.isEmpty() && objectListing.isTruncated());

                if (filteredObjectSummaries.isEmpty()) {
                    LOGGER.error("Amazon S3 object: {} has 0 sub-objects!", selectedObject);
                    throw new TestFailException(format("Amazon S3 object: %s has 0 sub-objects!", selectedObject));
                } else {
                    Log.log(LOGGER, format(" Amazon S3 object: %s contains %d sub-objects.",
                            selectedObject, filteredObjectSummaries.size()));
                }

                for (S3ObjectSummary objectSummary : filteredObjectSummaries.stream().limit(10).collect(Collectors.toList())) {
                    S3Object object = s3Client.getObject(bucketName, objectSummary.getKey());
                    S3ObjectInputStream inputStream = object.getObjectContent();

                    if (!zeroContent && object.getObjectMetadata().getContentLength() == 0) {
                        LOGGER.error("Amazon S3 path: {} has 0 bytes of content!", object.getKey());
                        throw new TestFailException(format("Amazon S3 path: %s has 0 bytes of content!", object.getKey()));
                    }
                    try {
                        inputStream.abort();
                        object.close();
                    } catch (IOException e) {
                        LOGGER.error("Unable to close Amazon S3 object {}. It has been returned with error!", object.getKey(), e);
                    }
                }
            } catch (AmazonServiceException e) {
                LOGGER.error("Amazon S3 couldn't process the call. So it has been returned with error!", e);
                throw new TestFailException("Amazon S3 couldn't process the call.", e);
            } catch (SdkClientException e) {
                LOGGER.error("Amazon S3 response could not been parsed, because of error!", e);
                throw new TestFailException("Amazon S3 response could not been parsed", e);
            } finally {
                s3Client.shutdown();
            }
        } else {
            LOGGER.error("Amazon S3 bucket is NOT present with name: {}", bucketName);
            throw new TestFailException("Amazon S3 bucket is NOT present with name: " + bucketName);
        }
    }

    public String getLoggingUrl(String baseLocation, String clusterLogPath) {
        URI baseLocationUri = getBaseLocationUri(baseLocation);
        String bucketName = getBucketName(baseLocation);
        String logPath = baseLocationUri.getPath();
        String deploymentS3Console = "https://s3.console.aws.amazon.com/s3/buckets/";

        Log.log(LOGGER, format(" Amazon S3 URI: %s", baseLocationUri));
        Log.log(LOGGER, format(" Amazon S3 Bucket: %s", bucketName));
        Log.log(LOGGER, format(" Amazon S3 Log Path: %s",  logPath));
        Log.log(LOGGER, format(" Amazon S3 Cluster Logs: %s", clusterLogPath));

        if (StringUtils.containsIgnoreCase(getAwsProperties().getRegion(), "us-gov")) {
            deploymentS3Console = "https://console.amazonaws-us-gov.com/s3/buckets/";
        }
        return format("%s%s?region=%s&prefix=%s%s/&showversions=false",
                deploymentS3Console, bucketName, getAwsProperties().getRegion(), getKeyPrefix(baseLocation), clusterLogPath);
    }
}
