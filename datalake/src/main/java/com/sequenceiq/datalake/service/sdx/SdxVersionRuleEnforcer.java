package com.sequenceiq.datalake.service.sdx;


import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Service
public class SdxVersionRuleEnforcer {

    public static final String MICRO_DUTY_REQUIRED_VERSION = "7.2.12";

    public static final String MEDIUM_DUTY_REQUIRED_VERSION = "7.2.7";

    public static final String MEDIUM_DUTY_MAXIMUM_VERSION = "7.2.17";

    public static final String ENTERPRISE_DATALAKE_REQUIRED_VERSION = "7.2.17";

    public static final String CCMV2_JUMPGATE_REQUIRED_VERSION = "7.2.6";

    public static final String CCMV2_REQUIRED_VERSION = "7.2.1";

    public static final Versioned MIN_RUNTIME_VERSION_FOR_RMS = () -> "7.2.18";

    public static final Versioned CUSTOM_ENCRYPTION_PROFILE_VERSION = () -> "7.3.2";

    private Map<CloudPlatform, Versioned> minRuntimeVersionForRaz;

    @PostConstruct
    public void configure() {
        minRuntimeVersionForRaz = new EnumMap<>(CloudPlatform.class);
        minRuntimeVersionForRaz.put(AWS, () -> "7.2.2");
        minRuntimeVersionForRaz.put(AZURE, () -> "7.2.2");
        minRuntimeVersionForRaz.put(GCP, () -> "7.2.17");
    }

    /**
     * Ranger Raz is only on 7.2.1 and later on Microsoft Azure, and only on 7.2.2 and later on Amazon Web Services.
     * If runtime is empty, then sdx-internal call was used.
     */
    protected boolean isRazSupported(String runtime, CloudPlatform cloudPlatform) {
        if (StringUtils.isEmpty(runtime)) {
            return true;
        }
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(() -> runtime, minRuntimeVersionForRaz.get(cloudPlatform)) > -1;
    }

    protected String getSupportedRazVersionForPlatform(CloudPlatform cloudPlatform) {
        return minRuntimeVersionForRaz.get(cloudPlatform).getVersion();
    }

    protected boolean isCustomEncryptionProfileSupported(String runtime) {
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(() -> runtime, CUSTOM_ENCRYPTION_PROFILE_VERSION) > -1;
    }
}
