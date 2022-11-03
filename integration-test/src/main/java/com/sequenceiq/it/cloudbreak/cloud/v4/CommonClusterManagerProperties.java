package com.sequenceiq.it.cloudbreak.cloud.v4;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Configuration
@ConfigurationProperties(prefix = "integrationtest")
public class CommonClusterManagerProperties {

    private final ClouderaManager clouderamanager = new ClouderaManager();

    private String runtimeVersion;

    private SdxClusterShape clusterShape;

    private SdxClusterShape internalClusterShape;

    private String internalSdxBlueprintName;

    private String internalDistroXBlueprintName;

    private UpgradeProperties upgrade = new UpgradeProperties();

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }

    public SdxClusterShape getClusterShape() {
        return clusterShape;
    }

    public void setClusterShape(SdxClusterShape clusterShape) {
        this.clusterShape = clusterShape;
    }

    public SdxClusterShape getInternalClusterShape() {
        return internalClusterShape;
    }

    public void setInternalClusterShape(SdxClusterShape internalClusterShape) {
        this.internalClusterShape = internalClusterShape;
    }

    public String getInternalSdxBlueprintName() {
        return String.format(internalSdxBlueprintName, runtimeVersion); }

    public void setInternalSdxBlueprintName(String internalSdxBlueprintName) {
        this.internalSdxBlueprintName = internalSdxBlueprintName;
    }

    public ClouderaManager getClouderaManager() {
        return clouderamanager;
    }

    public String getInternalDistroXBlueprintName() {
        return String.format(internalDistroXBlueprintName, runtimeVersion);
    }

    public void setInternalDistroXBlueprintName(String internalDistroXBlueprintName) {
        this.internalDistroXBlueprintName = internalDistroXBlueprintName;
    }

    public String getInternalDistroXBlueprintType() {
        return internalDistroXBlueprintName;
    }

    public UpgradeProperties getUpgrade() {
        return upgrade;
    }

    public static class ClouderaManager {

        private String defaultUser;

        private String defaultPassword;

        private String defaultPort;

        public String getDefaultUser() {
            return defaultUser;
        }

        public void setDefaultUser(String defaultUser) {
            this.defaultUser = defaultUser;
        }

        public String getDefaultPassword() {
            return defaultPassword;
        }

        public void setDefaultPassword(String defaultPassword) {
            this.defaultPassword = defaultPassword;
        }

        public String getDefaultPort() {
            return defaultPort;
        }

        public void setDefaultPort(String defaultPort) {
            this.defaultPort = defaultPort;
        }
    }

    public static class UpgradeProperties {
        private String currentHARuntimeVersion;

        private String currentRuntimeVersion;

        private String targetRuntimeVersion;

        private String distroXUpgradeCurrentVersion;

        private String distroXUpgradeTargetVersion;

        private String imageCatalogUrl3rdParty;

        private String distroXUpgrade3rdPartyCurrentVersion;

        private String distroXUpgrade3rdPartyTargetVersion;

        public String getCurrentHARuntimeVersion() {
            return currentHARuntimeVersion;
        }

        public void setCurrentHARuntimeVersion(String currentHARuntimeVersion) {
            this.currentHARuntimeVersion = currentHARuntimeVersion;
        }

        public String getCurrentRuntimeVersion() {
            return currentRuntimeVersion;
        }

        public void setCurrentRuntimeVersion(String currentRuntimeVersion) {
            this.currentRuntimeVersion = currentRuntimeVersion;
        }

        public String getTargetRuntimeVersion() {
            return targetRuntimeVersion;
        }

        public void setTargetRuntimeVersion(String targetRuntimeVersion) {
            this.targetRuntimeVersion = targetRuntimeVersion;
        }

        public String getDistroXUpgradeCurrentVersion() {
            return distroXUpgradeCurrentVersion;
        }

        public void setDistroXUpgradeCurrentVersion(String distroXUpgradeCurrentVersion) {
            this.distroXUpgradeCurrentVersion = distroXUpgradeCurrentVersion;
        }

        public String getDistroXUpgradeTargetVersion() {
            return distroXUpgradeTargetVersion;
        }

        public void setDistroXUpgradeTargetVersion(String distroXUpgradeTargetVersion) {
            this.distroXUpgradeTargetVersion = distroXUpgradeTargetVersion;
        }

        public String getImageCatalogUrl3rdParty() {
            return imageCatalogUrl3rdParty;
        }

        public void setImageCatalogUrl3rdParty(String imageCatalogUrl3rdParty) {
            this.imageCatalogUrl3rdParty = imageCatalogUrl3rdParty;
        }

        public String getDistroXUpgrade3rdPartyCurrentVersion() {
            return distroXUpgrade3rdPartyCurrentVersion;
        }

        public void setDistroXUpgrade3rdPartyCurrentVersion(String distroXUpgrade3rdPartyCurrentVersion) {
            this.distroXUpgrade3rdPartyCurrentVersion = distroXUpgrade3rdPartyCurrentVersion;
        }

        public String getDistroXUpgrade3rdPartyTargetVersion() {
            return distroXUpgrade3rdPartyTargetVersion;
        }

        public void setDistroXUpgrade3rdPartyTargetVersion(String distroXUpgrade3rdPartyTargetVersion) {
            this.distroXUpgrade3rdPartyTargetVersion = distroXUpgrade3rdPartyTargetVersion;
        }

    }
}
