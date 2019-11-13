package com.sequenceiq.it.cloudbreak.util.amazons3.action;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.amazons3.client.S3Client;

@Controller
public class S3ClientActions extends S3Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3ClientActions.class);

    public SdxTestDto deleteNonVersionedBucket(TestContext testContext, SdxTestDto sdxTestDto, SdxClient sdxClient) {
        AmazonS3 s3Client = buildS3Client();
        String bucketName = getBucketName();
        AmazonS3URI amazonS3URI = new AmazonS3URI(getEUWestS3Uri());
        String keyPrefix = getKeyPrefix(sdxTestDto);

        Log.log(LOGGER, format(" Amazon S3 URI: %s", amazonS3URI));
        Log.log(LOGGER, format(" Amazon S3 Bucket: %s", bucketName));
        Log.log(LOGGER, format(" Amazon S3 Key Prefix: %s", keyPrefix));

        if (s3Client.doesBucketExistV2(bucketName)) {
            try {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                        .withBucketName(bucketName)
                        .withPrefix(keyPrefix);
                ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);

                do {
                    for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                        Log.log(LOGGER, format(" Removing Amazon S3 Key with Name: %s and with bites of content %d",
                                objectSummary.getKey(), objectSummary.getSize()));
                        s3Client.deleteObject(new DeleteObjectRequest(bucketName, objectSummary.getKey()));
                    }
                    if (objectListing.isTruncated()) {
                        objectListing = s3Client.listNextBatchOfObjects(objectListing);
                    }
                } while (objectListing.isTruncated());
            } catch (AmazonServiceException e) {
                LOGGER.error("Amazon S3 couldn't process the call. So it has been returned with error!", e);
                throw new TestFailException(String.format("Amazon S3 couldn't process the call. So it has been returned the error: %s", e));
            } catch (SdkClientException e) {
                LOGGER.error("Amazon S3 response could not been parsed, because of error!", e);
                throw new TestFailException(String.format("Amazon S3 response could not been parsed, because of: %s", e));
            } finally {
                s3Client.shutdown();
            }
        } else {
            LOGGER.error("Amazon S3 bucket is not present with name: {}", bucketName);
            throw new TestFailException("Amazon S3 bucket is not present with name: " + bucketName);
        }

        return sdxTestDto;
    }

    public SdxTestDto listBucket(TestContext testContext, SdxTestDto sdxTestDto, SdxClient sdxClient) {
        AmazonS3 s3Client = buildS3Client();
        String bucketName = getBucketName();
        AmazonS3URI amazonS3URI = new AmazonS3URI(getEUWestS3Uri());
        String keyPrefix = getKeyPrefix(sdxTestDto);

        Log.log(LOGGER, format(" Amazon S3 URI: %s", amazonS3URI));
        Log.log(LOGGER, format(" Amazon S3 Bucket: %s", bucketName));
        Log.log(LOGGER, format(" Amazon S3 Key Prefix: %s", keyPrefix));

        if (s3Client.doesBucketExistV2(bucketName)) {
            try {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                        .withBucketName(bucketName)
                        .withPrefix(keyPrefix);
                ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);

                do {
                    for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                        Log.log(LOGGER, format(" Amazon S3 Key is present with Name: %s and with bites of content %d",
                                objectSummary.getKey(), objectSummary.getSize()));
                        s3Client.getObject(bucketName, objectSummary.getKey());
                    }
                    if (objectListing.isTruncated()) {
                        objectListing = s3Client.listNextBatchOfObjects(objectListing);
                    }
                } while (objectListing.isTruncated());
            } catch (AmazonServiceException e) {
                LOGGER.error("Amazon S3 couldn't process the call. So it has been returned with error!", e);
                throw new TestFailException(String.format("Amazon S3 couldn't process the call. So it has been returned the error: %s", e));
            } catch (SdkClientException e) {
                LOGGER.error("Amazon S3 response could not been parsed, because of error!", e);
                throw new TestFailException(String.format("Amazon S3 response could not been parsed, because of: %s", e));
            } finally {
                s3Client.shutdown();
            }
        } else {
            LOGGER.error("Amazon S3 bucket is NOT present with name: {}", bucketName);
            throw new TestFailException("Amazon S3 bucket is NOT present with name: " + bucketName);
        }

        return sdxTestDto;
    }
}
