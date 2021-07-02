package com.sequenceiq.it.cloudbreak.util.gcp;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;

@Component
public class GcpCloudFunctionality implements CloudFunctionality {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpCloudFunctionality.class);

    private static final String GCP_IMPLEMENTATION_MISSING = "GCP implementation missing";

    @Inject
    private GcpUtil gcpUtil;

    @Inject
    private GcpLabelUtil gcpLabelUtil;

    @Override
    public List<String> listInstanceVolumeIds(String clusterName, List<String> instanceIds) {
        return gcpUtil.listInstanceDiskNames(instanceIds);
    }

    @Override
    public Map<String, Map<String, String>> listTagsByInstanceId(String clusterName, List<String> instanceIds) {
        return gcpUtil.listTagsByInstanceId(instanceIds);
    }

    @Override
    public void deleteInstances(String clusterName, List<String> instanceIds) {
        gcpUtil.deleteHostGroupInstances(instanceIds);
    }

    @Override
    public void stopInstances(String clusterName, List<String> instanceIds) {
        gcpUtil.stopHostGroupInstances(instanceIds);
    }

    @Override
    public void cloudStorageInitialize() {
        LOGGER.debug("cloudStorageInitialize: nothing to do for GCP");
    }

    @Override
    public void cloudStorageListContainer(String baseLocation) {
        gcpUtil.cloudStorageListContainer(baseLocation);
    }

    @Override
    public void cloudStorageListContainerFreeIpa(String baseLocation, String clusterName, String crn) {
        gcpUtil.cloudStorageListContainerFreeIpa(baseLocation, clusterName, crn);
    }

    @Override
    public void cloudStorageListContainerDataLake(String baseLocation, String clusterName, String crn) {
        gcpUtil.cloudStorageListContainerDataLake(baseLocation, clusterName, crn);
    }

    @Override
    public void cloudStorageDeleteContainer(String baseLocation) {
        gcpUtil.cloudStorageDeleteContainer(baseLocation);
    }

    @Override
    public Map<String, Boolean> enaSupport(List<String> instanceIds) {
        return Collections.emptyMap();
    }

    @Override
    public String transformTagKeyOrValue(String originalValue) {
        return gcpLabelUtil.transformLabelKeyOrValue(originalValue);
    }
}
