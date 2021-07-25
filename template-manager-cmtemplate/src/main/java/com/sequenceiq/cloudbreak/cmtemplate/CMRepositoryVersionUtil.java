package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.Comparator;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.Versioned;

public class CMRepositoryVersionUtil {

    public static final Versioned CLOUDERAMANAGER_VERSION_6_3_0 = () -> "6.3.0";

    public static final Versioned CLOUDERAMANAGER_VERSION_6_4_0 = () -> "6.4.0";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_0_1 = () -> "7.0.1";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_0_2 = () -> "7.0.2";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_0_3 = () -> "7.0.3";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_1_0 = () -> "7.1.0";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_1_1 = () -> "7.1.1";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_2_0 = () -> "7.2.0";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_2_1 = () -> "7.2.1";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_2_2 = () -> "7.2.2";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_2_6 = () -> "7.2.6";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_2_7 = () -> "7.2.7";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_2_8 = () -> "7.2.8";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_4_1 = () -> "7.4.1";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_4_2 = () -> "7.4.2";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_4_3 = () -> "7.4.3";

    public static final Versioned CLOUDERA_STACK_VERSION_7_2_9 = () -> "7.2.9";

    public static final Versioned CLOUDERA_STACK_VERSION_7_2_10 = () -> "7.2.10";

    public static final Versioned CLOUDERA_STACK_VERSION_7_2_11 = () -> "7.2.11";

    public static final Versioned CLOUDERA_STACK_VERSION_7_2_12 = () -> "7.2.12";

    public static final Versioned CFM_VERSION_2_0_0_0 = () -> "2.0.0.0";

    public static final Versioned CDPD_VERSION_7_2_11 = () -> "7.2.11";

    private static final Logger LOGGER = LoggerFactory.getLogger(CMRepositoryVersionUtil.class);

    private CMRepositoryVersionUtil() {
    }

    public static boolean isEnableKerberosSupportedViaBlueprint(ClouderaManagerRepo clouderaManagerRepoDetails) {
        LOGGER.info("ClouderaManagerRepos compared for kerberos enablement");
        return isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_6_3_0);
    }

    public static boolean isKeepHostTemplateSupportedViaBlueprint(ClouderaManagerRepo clouderaManagerRepoDetails) {
        LOGGER.info("ClouderaManagerRepo is compared for host template");
        return isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_6_3_0);
    }

    public static boolean isKnoxGatewaySupported(ClouderaManagerRepo clouderaManagerRepoDetails) {
        LOGGER.info("ClouderaManagerRepo is compared for Knox Gateway support");
        return isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_6_4_0);
    }

    public static boolean isIdBrokerManagedIdentitySupported(ClouderaManagerRepo clouderaManagerRepoDetails) {
        LOGGER.info("ClouderaManagerRepo is compared for ID Broker managed identity support");
        return isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_0_2);
    }

    public static boolean isIgnorePropertyValidationSupportedViaBlueprint(ClouderaManagerRepo clouderaManagerRepoDetails) {
        LOGGER.info("ClouderaManagerRepo is compared for ignore porperty validation support");
        return isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_1_0);
    }

    public static boolean isTagsResourceSupportedViaBlueprint(ClouderaManagerRepo clouderaManagerRepoDetails) {
        LOGGER.info("ClouderaManagerRepo is compared for tags resource support");
        return isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_1_0);
    }

    public static boolean isRangerTearDownSupported(ClouderaManagerRepo clouderaManagerRepoDetails) {
        LOGGER.info("ClouderaManagerRepo is compared for ranger tear down support");
        return isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_4_1);
    }

    public static boolean isKnoxDatabaseSupported(ClouderaManagerRepo clouderaManagerRepoDetails,
        Optional<ClouderaManagerProduct> cdhProduct,
        Optional<Integer> cdhPatchVersion) {
        LOGGER.info("ClouderaManagerRepo is compared for knox database support");
        boolean supported = false;
        if (cdhProduct.isPresent()) {
            String cdhVersion = cdhProduct.get().getVersion().split("-")[0];
            if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERA_STACK_VERSION_7_2_10)) {
                if (isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_4_2)) {
                    supported = true;
                }
            } else if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERA_STACK_VERSION_7_2_9)) {
                if (cdhPatchVersion.isPresent() && cdhPatchVersion.get() >= 1
                        && isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_4_1)) {
                    supported = true;
                }
            }
        }
        return supported;
    }

    public static boolean isRazConfigurationSupportedInDatalake(ClouderaManagerRepo clouderaManagerRepoDetails, CloudPlatform cloudPlatform) {
        LOGGER.info("ClouderaManagerRepo is compared for Raz Ranger support in Datalake");
        return isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion,
                CloudPlatform.AWS == cloudPlatform ? CLOUDERAMANAGER_VERSION_7_2_2 : CLOUDERAMANAGER_VERSION_7_2_1);
    }

    public static boolean isRazConfigurationSupportedInDatahub(ClouderaManagerRepo clouderaManagerRepoDetails) {
        LOGGER.info("ClouderaManagerRepo is compared for Raz Ranger support in Datahub");
        return isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_2_2);
    }

    public static boolean isRazTokenConfigurationSupported(String cdhVersion, CloudPlatform cloudPlatform) {
        LOGGER.info("ClouderaManagerRepo is compared for Raz Ranger token support in Data Hub and Data Lake");
        return isVersionNewerOrEqualThanLimited(() -> cdhVersion, CLOUDERA_STACK_VERSION_7_2_10)
                && (cloudPlatform.equals(CloudPlatform.AWS) || cloudPlatform.equals(CloudPlatform.AZURE));
    }

    public static boolean isSudoAccessNeededForHostCertRotation(ClouderaManagerRepo clouderaManagerRepoDetails) {
        LOGGER.info("ClouderaManagerRepo is compared for Host certs rotation Sudo access");
        return isVersionOlderThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_2_6);
    }

    public static boolean isRootSshAccessNeededForHostCertRotation(ClouderaManagerRepo clouderaManagerRepoDetails) {
        LOGGER.info("ClouderaManagerRepo is compared for Host certs rotation root ssh access");
        return isVersionOlderThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_2_1);
    }

    public static boolean isVersionNewerOrEqualThanLimited(Versioned currentVersion, Versioned limitedAPIVersion) {
        LOGGER.info("Compared: Versioned {} with Versioned {}", currentVersion.getVersion(), limitedAPIVersion.getVersion());
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(currentVersion, limitedAPIVersion) > -1;
    }

    public static boolean isVersionNewerOrEqualThanLimited(String currentVersion, Versioned limitedAPIVersion) {
        LOGGER.info("Compared: String version {} with Versioned {}", currentVersion, limitedAPIVersion.getVersion());
        return isVersionNewerOrEqualThanLimited(() -> currentVersion, limitedAPIVersion);
    }

    public static boolean isVersionOlderThanLimited(Versioned currentVersion, Versioned limitedAPIVersion) {
        LOGGER.info("Compared: Versioned {} with Versioned {}", currentVersion.getVersion(), limitedAPIVersion.getVersion());
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(currentVersion, limitedAPIVersion) < 0;
    }

    public static boolean isVersionOlderThanLimited(String currentVersion, Versioned limitedAPIVersion) {
        LOGGER.info("Compared: String version {} with Versioned {}", currentVersion, limitedAPIVersion.getVersion());
        return isVersionOlderThanLimited(() -> currentVersion, limitedAPIVersion);
    }
}
