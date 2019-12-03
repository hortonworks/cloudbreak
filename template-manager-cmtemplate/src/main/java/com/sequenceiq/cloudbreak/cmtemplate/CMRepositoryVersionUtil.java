package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.Comparator;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.type.Versioned;

public class CMRepositoryVersionUtil {
    public static final Versioned CLOUDERAMANAGER_VERSION_6_3_0 = () -> "6.3.0";

    public static final Versioned CLOUDERAMANAGER_VERSION_6_4_0 = () -> "6.4.0";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_0_1 = () -> "7.0.1";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_0_2 = () -> "7.0.2";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_1_0 = () -> "7.1.0";

    public static final Versioned CFM_VERSION_2_0_0_0 = () -> "2.0.0.0";

    private CMRepositoryVersionUtil() {
    }

    public static boolean isEnableKerberosSupportedViaBlueprint(ClouderaManagerRepo clouderaManagerRepoDetails) {
        return isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_6_3_0);
    }

    public static boolean isKeepHostTemplateSupportedViaBlueprint(ClouderaManagerRepo clouderaManagerRepoDetails) {
        return isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_6_3_0);
    }

    public static boolean isKnoxGatewaySupported(ClouderaManagerRepo clouderaManagerRepoDetails) {
        return isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_6_4_0);
    }

    // TODO: Finalize if new version is released
    public static boolean isIdBrokerManagedIdentitySupported(ClouderaManagerRepo clouderaManagerRepoDetails) {
        return isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_1_0);

    }

    public static boolean isVersionNewerOrEqualThanLimited(Versioned currentVersion, Versioned limitedAPIVersion) {
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(currentVersion, limitedAPIVersion) > -1;
    }

    public static boolean isVersionNewerOrEqualThanLimited(String currentVersion, Versioned limitedAPIVersion) {
        return isVersionNewerOrEqualThanLimited(() -> currentVersion, limitedAPIVersion);
    }
}
