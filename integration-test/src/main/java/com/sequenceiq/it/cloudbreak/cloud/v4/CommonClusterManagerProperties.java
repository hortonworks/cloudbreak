package com.sequenceiq.it.cloudbreak.cloud.v4;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Configuration
@ConfigurationProperties(prefix = "integrationtest")
public class CommonClusterManagerProperties {

    private static final String FIRST_RUNTIME_VERSION_WITH_SPARK_VERSION = "7.2.18";

    private static final String SPARK_VERSION = "3";

    private final ClouderaManager clouderamanager = new ClouderaManager();

    private final VersionComparator versionComparator = new VersionComparator();

    private String runtimeVersion;

    private SdxClusterShape clusterShape;

    private SdxClusterShape internalClusterShape;

    private String internalSdxBlueprintName;

    private String dataEngDistroXBlueprintName;

    private String dataMartDistroXBlueprintName;

    private String streamsHADistroXBlueprintName;

    private UpgradeProperties upgrade = new UpgradeProperties();

    private UpgradeDatabaseServerProperties upgradeDatabaseServer = new UpgradeDatabaseServerProperties();

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
        return String.format(internalSdxBlueprintName, runtimeVersion);
    }

    public void setInternalSdxBlueprintName(String internalSdxBlueprintName) {
        this.internalSdxBlueprintName = internalSdxBlueprintName;
    }

    public String getInternalSdxBlueprintNameWithRuntimeVersion(String runtimeVersion) {
        return String.format(internalSdxBlueprintName, runtimeVersion);
    }

    public ClouderaManager getClouderaManager() {
        return clouderamanager;
    }

    public String getDataEngDistroXBlueprintNameForCurrentRuntime() {
        return getDataEngDistroXBlueprintName(runtimeVersion);
    }

    public String getDataEngDistroXBlueprintName(String distroXUpgradeCurrentVersion) {
        return String.format(dataEngDistroXBlueprintName, distroXUpgradeCurrentVersion, getSparkVersion(distroXUpgradeCurrentVersion));
    }

    private String getSparkVersion(String distroXUpgradeCurrentVersion) {
        return versionComparator.compare(() -> distroXUpgradeCurrentVersion, () -> FIRST_RUNTIME_VERSION_WITH_SPARK_VERSION) >= 0 ? SPARK_VERSION : "";
    }

    public void setDataEngDistroXBlueprintName(String dataEngDistroXBlueprintName) {
        this.dataEngDistroXBlueprintName = dataEngDistroXBlueprintName;
    }

    public String getDataMartDistroXBlueprintNameForCurrentRuntime() {
        return String.format(dataMartDistroXBlueprintName, runtimeVersion);
    }

    public String getDataMartDistroXBlueprintName(String runtimeVersion) {
        return String.format(dataMartDistroXBlueprintName, runtimeVersion);
    }

    public void setDataMartDistroXBlueprintName(String dataMartDistroXBlueprintName) {
        this.dataMartDistroXBlueprintName = dataMartDistroXBlueprintName;
    }

    public String getStreamsHADistroXBluepringNameForCurrentRuntime() {
        return getStreamsHADistroXBlueprintName(runtimeVersion);
    }

    public String getStreamsHADistroXBlueprintName(String distroXUpgradeCurrentVersion) {
        return String.format(streamsHADistroXBlueprintName, distroXUpgradeCurrentVersion);
    }

    public void setStreamsHADistroXBlueprintName(String streamsHADistroXBlueprintName) {
        this.streamsHADistroXBlueprintName = streamsHADistroXBlueprintName;
    }

    public UpgradeProperties getUpgrade() {
        return upgrade;
    }

    public UpgradeDatabaseServerProperties getUpgradeDatabaseServer() {
        return upgradeDatabaseServer;
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
        private Map<String, String> matrix;

        private String currentHARuntimeVersion;

        private String currentGovHARuntimeVersion;

        private String currentRuntimeVersion;

        private String currentGovRuntimeVersion;

        private String targetRuntimeVersion;

        private String distroXUpgradeCurrentVersion;

        private String distroXUpgradeCurrentGovVersion;

        private String distroXUpgradeTargetVersion;

        private String imageCatalogUrl3rdParty;

        private String distroXUpgrade3rdPartyCurrentVersion;

        private String distroXUpgrade3rdPartyCurrentGovVersion;

        private String distroXUpgrade3rdPartyTargetVersion;

        public Map<String, String> getMatrix() {
            return matrix;
        }

        public void setMatrix(Map<String, String> matrix) {
            this.matrix = matrix;
        }

        public String getCurrentGovHARuntimeVersion() {
            return currentGovHARuntimeVersion;
        }

        public void setCurrentGovHARuntimeVersion(String currentGovHARuntimeVersion) {
            this.currentGovHARuntimeVersion = currentGovHARuntimeVersion;
        }

        public String getDistroXUpgradeCurrentGovVersion() {
            return distroXUpgradeCurrentGovVersion;
        }

        public void setDistroXUpgradeCurrentGovVersion(String distroXUpgradeCurrentGovVersion) {
            this.distroXUpgradeCurrentGovVersion = distroXUpgradeCurrentGovVersion;
        }

        public String getCurrentGovRuntimeVersion() {
            return currentGovRuntimeVersion;
        }

        public void setCurrentGovRuntimeVersion(String currentGovRuntimeVersion) {
            this.currentGovRuntimeVersion = currentGovRuntimeVersion;
        }

        public String getDistroXUpgrade3rdPartyCurrentGovVersion() {
            return distroXUpgrade3rdPartyCurrentGovVersion;
        }

        public void setDistroXUpgrade3rdPartyCurrentGovVersion(String distroXUpgrade3rdPartyCurrentGovVersion) {
            this.distroXUpgrade3rdPartyCurrentGovVersion = distroXUpgrade3rdPartyCurrentGovVersion;
        }

        public String getCurrentHARuntimeVersion(boolean govCloud) {
            return govCloud ? currentGovHARuntimeVersion : currentHARuntimeVersion;
        }

        public void setCurrentHARuntimeVersion(String currentHARuntimeVersion) {
            this.currentHARuntimeVersion = currentHARuntimeVersion;
        }

        public String getCurrentRuntimeVersion(boolean govCloud) {
            return govCloud ? currentGovRuntimeVersion : currentRuntimeVersion;
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

        public String getDistroXUpgradeCurrentVersion(boolean govCloud) {
            return govCloud ? distroXUpgradeCurrentGovVersion : distroXUpgradeCurrentVersion;
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

        public String getDistroXUpgrade3rdPartyCurrentVersion(boolean govCloud) {
            return govCloud ? distroXUpgrade3rdPartyCurrentGovVersion : distroXUpgrade3rdPartyCurrentVersion;
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

    public static class UpgradeDatabaseServerProperties {
        private String originalDatabaseMajorVersion;

        private String targetDatabaseMajorVersion;

        private DatabaseVersionProperties latest;

        public String getOriginalDatabaseMajorVersion() {
            return originalDatabaseMajorVersion;
        }

        public void setOriginalDatabaseMajorVersion(String originalDatabaseMajorVersion) {
            this.originalDatabaseMajorVersion = originalDatabaseMajorVersion;
        }

        public String getTargetDatabaseMajorVersion() {
            return targetDatabaseMajorVersion;
        }

        public void setTargetDatabaseMajorVersion(String targetDatabaseMajorVersion) {
            this.targetDatabaseMajorVersion = targetDatabaseMajorVersion;
        }

        public DatabaseVersionProperties getLatest() {
            return latest;
        }

        public void setLatest(DatabaseVersionProperties latest) {
            this.latest = latest;
        }
    }

    public static class DatabaseVersionProperties {
        private String runtimeVersion;

        private String originalDatabaseMajorVersion;

        private String targetDatabaseMajorVersion;

        public String getOriginalDatabaseMajorVersion() {
            return originalDatabaseMajorVersion;
        }

        public void setOriginalDatabaseMajorVersion(String originalDatabaseMajorVersion) {
            this.originalDatabaseMajorVersion = originalDatabaseMajorVersion;
        }

        public String getTargetDatabaseMajorVersion() {
            return targetDatabaseMajorVersion;
        }

        public void setTargetDatabaseMajorVersion(String targetDatabaseMajorVersion) {
            this.targetDatabaseMajorVersion = targetDatabaseMajorVersion;
        }

        public String getRuntimeVersion() {
            return runtimeVersion;
        }

        public void setRuntimeVersion(String runtimeVersion) {
            this.runtimeVersion = runtimeVersion;
        }
    }
}
