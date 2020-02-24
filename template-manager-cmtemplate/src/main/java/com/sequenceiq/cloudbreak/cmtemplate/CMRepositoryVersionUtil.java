package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.type.Versioned;

public class CMRepositoryVersionUtil {

    public static final Versioned CLOUDERAMANAGER_VERSION_6_3_0 = () -> "6.3.0";

    public static final Versioned CLOUDERAMANAGER_VERSION_6_4_0 = () -> "6.4.0";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_0_1 = () -> "7.0.1";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_0_2 = () -> "7.0.2";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_1_0 = () -> "7.1.0";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_0_3 = () -> "7.0.3";

    public static final Versioned CFM_VERSION_2_0_0_0 = () -> "2.0.0.0";

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

    public static boolean isVersionNewerOrEqualThanLimited(Versioned currentVersion, Versioned limitedAPIVersion) {
        LOGGER.info("Compared: Versioned {} with Versioned {}", currentVersion.getVersion(), limitedAPIVersion.getVersion());
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(currentVersion, limitedAPIVersion) > -1;
    }

    public static boolean isVersionNewerOrEqualThanLimited(String currentVersion, Versioned limitedAPIVersion) {
        LOGGER.info("Compared: String version {} with Versioned {}", currentVersion, limitedAPIVersion.getVersion());
        return isVersionNewerOrEqualThanLimited(() -> currentVersion, limitedAPIVersion);
    }
}
