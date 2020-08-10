package com.sequenceiq.it.cloudbreak.util;

import java.util.List;
import java.util.Map;

public interface CloudFunctionality {
    List<String> listInstanceVolumeIds(List<String> instanceIds);

    Map<String, Map<String, String>> listTagsByInstanceId(List<String> instanceIds);

    void deleteInstances(List<String> instanceIds);

    void stopInstances(List<String> instanceIds);

    void cloudStorageInitialize();

    void cloudStorageListContainer(String baseLocation);

    void cloudStorageListContainerFreeIpa(String baseLocation, String clusterName, String crn);

    void cloudStorageListContainerDataLake(String baseLocation, String clusterName, String crn);

    void cloudStorageDeleteContainer(String baseLocation);

}
