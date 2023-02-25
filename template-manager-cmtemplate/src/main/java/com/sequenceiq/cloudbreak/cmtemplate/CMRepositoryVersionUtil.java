package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;

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

    public static final Versioned CLOUDERAMANAGER_VERSION_7_5_1 = () -> "7.5.1";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_6_0 = () -> "7.6.0";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_7_1 = () -> "7.7.1";

    public static final Versioned CLOUDERAMANAGER_VERSION_7_9_0 = () -> "7.9.0";

    public static final Versioned CLOUDERA_STACK_VERSION_7_2_7 = () -> "7.2.7";

    public static final Versioned CLOUDERA_STACK_VERSION_7_2_9 = () -> "7.2.9";

    public static final Versioned CLOUDERA_STACK_VERSION_7_2_10 = () -> "7.2.10";

    public static final Versioned CLOUDERA_STACK_VERSION_7_2_11 = () -> "7.2.11";

    public static final Versioned CLOUDERA_STACK_VERSION_7_2_12 = () -> "7.2.12";

    public static final Versioned CLOUDERA_STACK_VERSION_7_2_14 = () -> "7.2.14";

    public static final Versioned CLOUDERA_STACK_VERSION_7_2_16 = () -> "7.2.16";

    public static final Versioned CFM_VERSION_2_0_0_0 = () -> "2.0.0.0";

    public static final Versioned CFM_VERSION_2_2_3_0 = () -> "2.2.3.0";

    public static final Versioned CDPD_VERSION_7_2_11 = () -> "7.2.11";

    public static final Map<CloudPlatform, Versioned> MIN_CM_VERSION_FOR_RAZ = new HashMap<>() {
        {
            put(AWS, CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2);
            put(AZURE, CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2);
            put(GCP, CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_9_0);
        }
    };

    public static final Map<CloudPlatform, List<StackType>> RAZ_ENABLED_CLOUD_PLATFORMS = new HashMap<>() {
        {
            put(AWS, List.of(StackType.DATALAKE, StackType.WORKLOAD));
            put(AZURE, List.of(StackType.DATALAKE, StackType.WORKLOAD));
            put(GCP, List.of(StackType.DATALAKE));
        }
    };

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
            LOGGER.info("The cdhVersion is {}", cdhVersion);
            if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERA_STACK_VERSION_7_2_10)) {
                LOGGER.info("The cdhVersion {} is newer or equal then {}", cdhVersion, CLOUDERA_STACK_VERSION_7_2_10.getVersion());
                if (isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_4_2)) {
                    LOGGER.info("The cmVersion {} is newer or equal then {}",
                            clouderaManagerRepoDetails.getVersion(), CLOUDERAMANAGER_VERSION_7_4_2.getVersion());
                    supported = true;
                }
            } else if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERA_STACK_VERSION_7_2_9)) {
                LOGGER.info("The cdhVersion {} is newer or equal then {}", cdhVersion, CLOUDERA_STACK_VERSION_7_2_9.getVersion());
                if (cdhPatchVersion.isPresent() && cdhPatchVersion.get() >= 1
                        && isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_4_1)) {
                    LOGGER.info("The cmVersion {} is newer or equal then {} and the patch version is acceptable",
                            clouderaManagerRepoDetails.getVersion(), CLOUDERAMANAGER_VERSION_7_4_1.getVersion());
                    supported = true;
                }
            }
        }
        LOGGER.info("The current cdh and cm version is supported={}", supported);
        return supported;
    }

    public static boolean isRazConfigurationSupported(String cmVersion, CloudPlatform cloudPlatform, StackType stackType) {
        LOGGER.info("CM Version {} is compared for Raz support for {} in {}", cmVersion, cloudPlatform, stackType);
        return isRazSupportedForCloudAndStack(cloudPlatform, stackType)
                && isVersionNewerOrEqualThanLimited(cmVersion, (MIN_CM_VERSION_FOR_RAZ).get(cloudPlatform));
    }

    public static boolean isRazSupportedForCloudAndStack(CloudPlatform cloudPlatform, StackType stackType) {
        LOGGER.info("Cloud Platform {} is compared for Raz support in {}", cloudPlatform, stackType);
        return RAZ_ENABLED_CLOUD_PLATFORMS.getOrDefault(cloudPlatform, Collections.emptyList()).contains(stackType);
    }

    public static boolean isRazTokenConfigurationSupported(String cdhVersion, CloudPlatform cloudPlatform, StackType stackType) {
        LOGGER.info("ClouderaManagerRepo is compared for Raz Ranger token support in Data Hub and Data Lake");
        return isVersionNewerOrEqualThanLimited(() -> cdhVersion, CLOUDERA_STACK_VERSION_7_2_10)
                && isRazSupportedForCloudAndStack(cloudPlatform, stackType);
    }

    public static boolean isSudoAccessNeededForHostCertRotation(ClouderaManagerRepo clouderaManagerRepoDetails) {
        LOGGER.info("ClouderaManagerRepo is compared for Host certs rotation Sudo access");
        return isVersionOlderThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_2_6);
    }

    public static boolean isRootSshAccessNeededForHostCertRotation(ClouderaManagerRepo clouderaManagerRepoDetails) {
        LOGGER.info("ClouderaManagerRepo is compared for Host certs rotation root ssh access");
        return isVersionOlderThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_2_1);
    }

    public static boolean isCmServicesHealthCheckAllowed(Optional<String> runtimeVersion) {
        if (runtimeVersion.isPresent()) {
            LOGGER.info("isCmServicesHealthCheckAllowed Runtime version is compared for CM services health check.");
            return isVersionNewerOrEqualThanLimited(runtimeVersion.get(), CLOUDERA_STACK_VERSION_7_2_12);
        }
        return false;
    }

    public static boolean isCmBulkHostsRemovalAllowed(Optional<String> runtimeVersion) {
        if (runtimeVersion.isPresent()) {
            LOGGER.info("isCmBulkHostsRemovalAllowed Runtime version is compared for CM bulk hosts removal.");
            return isVersionNewerOrEqualThanLimited(runtimeVersion.get(), CLOUDERA_STACK_VERSION_7_2_14);
        }
        return false;
    }

    public static boolean isVersionNewerOrEqualThanLimited(Versioned currentVersion, Versioned limitedAPIVersion) {
        LOGGER.info("isVersionNewerOrEqualThanLimited Compared: Versioned {} with Versioned {}", currentVersion.getVersion(), limitedAPIVersion.getVersion());
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(currentVersion, limitedAPIVersion) > -1;
    }

    public static boolean isVersionNewerOrEqualThanLimited(String currentVersion, Versioned limitedAPIVersion) {
        LOGGER.info("isVersionNewerOrEqualThanLimited Compared: String version {} with Versioned {}",
                currentVersion, limitedAPIVersion.getVersion());
        return isVersionNewerOrEqualThanLimited(() -> currentVersion, limitedAPIVersion);
    }

    public static boolean isVersionOlderThanLimited(Versioned currentVersion, Versioned limitedAPIVersion) {
        LOGGER.info("isVersionOlderThanLimited Compared: Versioned {} with Versioned {}",
                currentVersion.getVersion(), limitedAPIVersion.getVersion());
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(currentVersion, limitedAPIVersion) < 0;
    }

    public static boolean isVersionOlderThanLimited(String currentVersion, Versioned limitedAPIVersion) {
        LOGGER.info("isVersionOlderThanLimited Compared: String version {} with Versioned {}",
                currentVersion, limitedAPIVersion.getVersion());
        return isVersionOlderThanLimited(() -> currentVersion, limitedAPIVersion);
    }
}
