package com.sequenceiq.it.cloudbreak.util.aws.amazons3;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
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

    public void listBucketSelectedObject(String baseLocation, String selectedObject, boolean zeroContent) {
        s3ClientActions.listBucketSelectedObject(baseLocation, selectedObject, zeroContent);
    }

    public void listFreeIpaObject(String baseLocation) {
        s3ClientActions.listBucketSelectedObject(baseLocation, "cluster-logs/freeipa",
                false);
    }

    public void listDataLakeObject(String baseLocation) {
        s3ClientActions.listBucketSelectedObject(baseLocation, "cluster-logs/datalake",
                false);
    }

    public String getFreeIpaLogsUrl(String clusterName, String crn, String baseLocation) {
        return s3ClientActions.getLoggingUrl(baseLocation, "/cluster-logs/freeipa/" + clusterName + "_" + Crn.fromString(crn).getResource());
    }

    public String getDataLakeLogsUrl(String clusterName, String crn, String baseLocation) {
        return s3ClientActions.getLoggingUrl(baseLocation, "/cluster-logs/datalake/" + clusterName + "_" + Crn.fromString(crn).getResource());
    }

    public String getDataHubLogsUrl(String clusterName, String crn, String baseLocation) {
        return s3ClientActions.getLoggingUrl(baseLocation, "/cluster-logs/datahub/" + clusterName + "_" + Crn.fromString(crn).getResource());
    }
}
