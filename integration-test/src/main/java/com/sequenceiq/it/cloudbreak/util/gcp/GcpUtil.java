package com.sequenceiq.it.cloudbreak.util.gcp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
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

    public void cloudStorageListContainer(String baseLocation, String selectedObject, boolean zeroContent) {
        listSelectedObject(baseLocation + selectedObject, zeroContent);
    }

    public void cloudStorageListContainerFreeIpa(String baseLocation, String clusterName, String crn) {
        listSelectedObject(baseLocation + "/cluster-logs/freeipa");
    }

    public void cloudStorageListContainerDataLake(String baseLocation, String clusterName, String crn) {
        listSelectedObject(baseLocation + "/cluster-logs/datalake");
    }

    public void cloudStorageDeleteContainer(String baseLocation) {
        String bucketName = gcpStackUtil.getBucketName(baseLocation);
        gcpClientActions.deleteNonVersionedBucket(bucketName);
    }

    private void listSelectedObject(String baseLocation) {
        listSelectedObject(baseLocation, false);
    }

    private void listSelectedObject(String baseLocation, boolean zeroContent) {
        try {
            URI baseLocationUri = new URI(baseLocation);
            gcpClientActions.listBucketSelectedObject(baseLocationUri, zeroContent);
        } catch (URISyntaxException e) {
            LOGGER.error("Google GCS base location path: '{}' is not a valid URI!", baseLocation);
            throw new TestFailException(String.format(" Google GCS base location path: '%s' is not a valid URI! ", baseLocation));
        }
    }
}
