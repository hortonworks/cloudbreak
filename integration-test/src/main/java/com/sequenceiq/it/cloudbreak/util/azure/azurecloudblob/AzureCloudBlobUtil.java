package com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob.action.AzureCloudBlobClientActions;

@Component
public class AzureCloudBlobUtil {

    @Inject
    private AzureCloudBlobClientActions azureCloudBlobClientActions;

    private AzureCloudBlobUtil() {
    }

    public void cleanupContainer(String baseLocation) {
        azureCloudBlobClientActions.cleanupContainer(baseLocation);
    }

    public void createContainerIfNotExist() {
        azureCloudBlobClientActions.createBlobContainerClient();
    }

    public void listSelectedFoldersInAContainer(String baseLocation, String selectedDirectory, boolean zeroContent) {
        azureCloudBlobClientActions.listSelectedDirectory(baseLocation, selectedDirectory, zeroContent);
    }

    public void listDataLakeFoldersInAContainer(String baseLocation, String clusterName, String crn) {
        azureCloudBlobClientActions.listSelectedDirectory(baseLocation,
                "cluster-logs/datalake/" + clusterName + "_" + Crn.fromString(crn).getResource(), false);
    }

    public void listFreeIpaFoldersInAContainer(String baseLocation, String clusterName, String crn) {
        azureCloudBlobClientActions.listSelectedDirectory(baseLocation,
                "cluster-logs/freeipa/" + clusterName + "_" + Crn.fromString(crn).getResource(), false);
    }

    public String getFreeIpaLogsUrl(String clusterName, String crn, String baseLocation) {
        return azureCloudBlobClientActions.getLoggingUrl(baseLocation, "/cluster-logs/freeipa/" + clusterName + "_" + Crn.fromString(crn).getResource());
    }

    public String getDataLakeLogsUrl(String clusterName, String crn, String baseLocation) {
        return azureCloudBlobClientActions.getLoggingUrl(baseLocation, "/cluster-logs/datalake/" + clusterName + "_" + Crn.fromString(crn).getResource());
    }

    public String getDataHubLogsUrl(String clusterName, String crn, String baseLocation) {
        return azureCloudBlobClientActions.getLoggingUrl(baseLocation, "/cluster-logs/datahub/" + clusterName + "_" + Crn.fromString(crn).getResource());
    }

}
