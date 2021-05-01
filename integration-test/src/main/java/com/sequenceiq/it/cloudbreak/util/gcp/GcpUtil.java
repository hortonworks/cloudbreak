package com.sequenceiq.it.cloudbreak.util.gcp;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.it.cloudbreak.util.gcp.action.GcpClientActions;

@Component
public class GcpUtil {

    @Inject
    private GcpClientActions gcpClientActions;

    @Inject
    private GcpStackUtil gcpStackUtil;

    private GcpUtil() {
    }

    public List<String> listInstanceDiskNames(List<String> instanceIds) {
        return gcpClientActions.listInstanceDiskNames(instanceIds);
    }

    public Map<String, Map<String, String>> listTagsByInstanceId(List<String> instanceIds) {
        return gcpClientActions.listTagsByInstanceId(instanceIds);
    }

    public void deleteHostGroupInstances(List<String> instanceIds) {
        gcpClientActions.deleteHostGroupInstances(instanceIds);
    }

    public void stopHostGroupInstances(List<String> instanceIds) {
        gcpClientActions.stopHostGroupInstances(instanceIds);
    }

    public void cloudStorageListContainer(String baseLocation) {
        listSelectedObject(baseLocation);
    }

    public void cloudStorageListContainerFreeIpa(String baseLocation, String clusterName, String crn) {
        listSelectedObject(baseLocation);
    }

    public void cloudStorageListContainerDataLake(String baseLocation, String clusterName, String crn) {
        listSelectedObject(baseLocation);
    }

    public void cloudStorageDeleteContainer(String baseLocation) {
        String bucketName = gcpStackUtil.getBucketName(baseLocation);
        gcpClientActions.deleteNonVersionedBucket(bucketName);
    }

    private void listSelectedObject(String baseLocation) {
        String bucketName = gcpStackUtil.getBucketName(baseLocation);
        String objectPath = gcpStackUtil
                .getPath(baseLocation)
                .replace(bucketName + "/", "")
                + "/";
        gcpClientActions.listBucketSelectedObject(bucketName, objectPath, false);
    }
}
