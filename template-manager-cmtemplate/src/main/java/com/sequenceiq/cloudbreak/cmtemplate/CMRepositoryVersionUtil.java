package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.Comparator;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Versioned;

public class CMRepositoryVersionUtil {
    public static final Versioned CLOUDERAMANAGER_VERSION_6_3_0 = () -> "6.3.0";

    private CMRepositoryVersionUtil() {
    }

    public static boolean isEnableKerberosSupportedViaBlueprint(ClouderaManagerRepo clouderaManagerRepoDetails) {
        return isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_6_3_0);
    }

    public static boolean isKeepHostTemplateSupportedViaBlueprint(ClouderaManagerRepo clouderaManagerRepoDetails) {
        return isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_6_3_0);
    }

    public static boolean isVersionNewerOrEqualThanLimited(Versioned currentVersion, Versioned limitedAPIVersion) {
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(currentVersion, limitedAPIVersion) > -1;
    }
}
