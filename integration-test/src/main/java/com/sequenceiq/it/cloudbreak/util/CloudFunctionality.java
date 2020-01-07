package com.sequenceiq.it.cloudbreak.util;

import java.util.List;

public interface CloudFunctionality {
    List<String> listInstanceVolumeIds(List<String> instanceIds);

    void deleteInstances(List<String> instanceIds);

    void stopInstances(List<String> instanceIds);

    void cloudStorageInitialize();

    void cloudStorageListContainer(String baseLocation);

    void cloudStorageDeleteContainer(String baseLocation);

}
