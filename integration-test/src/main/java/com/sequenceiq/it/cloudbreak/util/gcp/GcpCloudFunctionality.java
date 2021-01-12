package com.sequenceiq.it.cloudbreak.util.gcp;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
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

    @Override
    public List<String> listInstanceVolumeIds(List<String> instanceIds) {
        return gcpUtil.listInstanceDiskNames(instanceIds);
    }

    @Override
    public Map<String, Map<String, String>> listTagsByInstanceId(List<String> instanceIds) {
        return gcpUtil.listTagsByInstanceId(instanceIds);
    }

    @Override
    public void deleteInstances(List<String> instanceIds) {
        gcpUtil.deleteHostGroupInstances(instanceIds);
    }

    @Override
    public void stopInstances(List<String> instanceIds) {
        gcpUtil.stopHostGroupInstances(instanceIds);
    }

    @Override
    public void cloudStorageInitialize() {
        LOGGER.debug("cloudStorageInitialize: nothing to do for GCP");
    }

    @Override
    public void cloudStorageListContainer(String baseLocation) {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }

    @Override
    public void cloudStorageListContainerFreeIpa(String baseLocation, String clusterName, String crn) {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }

    @Override
    public void cloudStorageListContainerDataLake(String baseLocation, String clusterName, String crn) {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }

    @Override
    public void cloudStorageDeleteContainer(String baseLocation) {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }

    @Override
    public Map<String, Boolean> enaSupport(List<String> instanceIds) {
        return Collections.emptyMap();
    }

    @Override
    public String transformTagKeyOrValue(String originalValue) {
        return GcpLabelUtil.transformLabelKeyOrValue(originalValue);
    }
}
