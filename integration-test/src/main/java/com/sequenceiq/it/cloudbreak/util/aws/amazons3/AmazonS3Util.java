package com.sequenceiq.it.cloudbreak.util.aws.amazons3;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.util.aws.amazons3.action.S3ClientActions;

@Component
public class AmazonS3Util {
    @Inject
    private S3ClientActions s3ClientActions;

    private AmazonS3Util() {
    }

    public void deleteNonVersionedBucket(String baseLocation) {
        s3ClientActions.deleteNonVersionedBucket(baseLocation);
    }

    public void listBucket(String baseLocation) {
        s3ClientActions.listBucketSelectedObject(baseLocation, "ranger", true);
    }

    public void listFreeIPAObject(String baseLocation) {
        s3ClientActions.listBucketSelectedObject(baseLocation, "freeipa", false);
    }

    public void listDataLakeObject(String baseLocation) {
        s3ClientActions.listBucketSelectedObject(baseLocation, "datalake", false);
    }
}
