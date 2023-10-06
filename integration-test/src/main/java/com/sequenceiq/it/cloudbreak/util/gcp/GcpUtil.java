package com.sequenceiq.it.cloudbreak.util.gcp;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.it.cloudbreak.util.gcp.action.GcpClientActions;

@Component
public class GcpUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpUtil.class);

    @Inject
    private GcpClientActions gcpClientActions;

    @Inject
    private GcpStackUtil gcpStackUtil;

    private GcpUtil() {
    }

    public List<String> listInstancesDiskNames(List<String> instanceIds) {
        return gcpClientActions.getSelectedInstancesDiskNames(instanceIds);
    }

    public Map<String, Set<String>> listInstanceVolumeIds(String instanceId) {
        return gcpClientActions.getInstanceDiskNames(instanceId);
    }

    public List<String> listInstanceTypes(List<String> instanceIds) {
        return gcpClientActions.listInstanceTypes(instanceIds);
    }

    public List<String> listVolumeEncryptionKey(List<String> instanceIds) {
        return gcpClientActions.listVolumeEncryptionKey(instanceIds);
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

    public void cloudStorageListContainer(String baseLocation, String selectedObject, boolean zeroContent) {
        gcpClientActions.listBucketSelectedObject(baseLocation, selectedObject, zeroContent);
    }

    public void cloudStorageListContainerFreeIpa(String baseLocation, String clusterName, String crn) {
        gcpClientActions.listBucketSelectedObject(baseLocation, "cluster-logs/freeipa", false);
    }

    public void cloudStorageListContainerDataLake(String baseLocation, String clusterName, String crn) {
        gcpClientActions.listBucketSelectedObject(baseLocation, "cluster-logs/datalake", false);
    }

    public void cloudStorageDeleteContainer(String baseLocation) {
        String bucketName = gcpStackUtil.getBucketName(baseLocation);
        gcpClientActions.deleteNonVersionedBucket(bucketName);
    }

    public String getFreeIpaLogsUrl(String clusterName, String crn, String baseLocation) {
        return gcpClientActions.getLoggingUrl(baseLocation, "/cluster-logs/freeipa/" + clusterName + "_" + Crn.fromString(crn).getResource());
    }

    public String getDataLakeLogsUrl(String clusterName, String crn, String baseLocation) {
        return gcpClientActions.getLoggingUrl(baseLocation, "/cluster-logs/datalake/" + clusterName + "_" + Crn.fromString(crn).getResource());
    }

    public String getDataHubLogsUrl(String clusterName, String crn, String baseLocation) {
        return gcpClientActions.getLoggingUrl(baseLocation, "/cluster-logs/datahub/" + clusterName + "_" + Crn.fromString(crn).getResource());
    }
}
