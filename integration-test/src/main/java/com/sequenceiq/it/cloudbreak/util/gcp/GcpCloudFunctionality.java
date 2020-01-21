package com.sequenceiq.it.cloudbreak.util.gcp;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;

public class GcpCloudFunctionality implements CloudFunctionality {

    private static final String GCP_IMPLEMENTATION_MISSING = "GCP implementation missing";

    @Override
    public List<String> listInstanceVolumeIds(List<String> instanceIds) {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }

    @Override
    public void deleteInstances(List<String> instanceIds) {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }

    @Override
    public void stopInstances(List<String> instanceIds) {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }

    @Override
    public void cloudStorageInitialize() {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }

    @Override
    public void cloudStorageListContainer(String baseLocation) {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }

    @Override
    public void cloudStorageListContainerFreeIPA(String baseLocation) {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }

    @Override
    public void cloudStorageListContainerDataLake(String baseLocation) {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }

    @Override
    public void cloudStorageDeleteContainer(String baseLocation) {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }
}
