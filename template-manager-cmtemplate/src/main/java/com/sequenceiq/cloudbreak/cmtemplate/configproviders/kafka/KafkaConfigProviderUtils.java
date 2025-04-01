package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_3;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.EnumSet;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

public class KafkaConfigProviderUtils {

    private KafkaConfigProviderUtils() {
    }

    /*
    TODO: This logic needs to be revised whenever a new CDH version comes out.
    Some background on this rather complicated versioning scheme:
    - CDH 7.0.2 contained partial support for Kafka + streaming, not officially released
    - Full support was originally targeted in 7.1.0
    - CDH 7.0.3 (datacenter edition) came out with the partial streaming support in 7.0.2
    - CDH 7.1.0 was delayed and as a result, full support was re-targeted to 7.0.2.2
    This has lead to the current situation, where the 7.0.2.2 version is based on newer code than the 7.0.3 version.
    Since it is difficult to predict future CDH releases and their content, the following is assumed on a best-effort basis:
    - 7.0.x (7.0.4, 7.0.5, ...) versions will use the same logic as 7.0.2 and 7.0.3
    - 7.1.0 and later versions will use the logic in 7.0.2.2
    Update for 7.1.0:
    - 7.1.0 introduced SSL connection between Kafka and Zookeeper. Due to a CM issue, the Ranger Kafka plugin's Zookeeper
    config doesn't use the SSL port, causing problems for Ranger connecting to Zookeeper.
     */
    public static CdhVersionForStreaming getCdhVersionForStreaming(TemplatePreparationObject source) {
        String cdhVersion = ConfigUtils.getCdhVersion(source);
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_2_0)) {
            return CdhVersionForStreaming.VERSION_7_X_X;
        } else if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_1_0)) {
            return CdhVersionForStreaming.VERSION_7_1_0;
        } else if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_0_3)) {
            return CdhVersionForStreaming.VERSION_7_0_X;
        } else if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_0_2)) {
            Optional<Integer> patchVersion = getCdhPatchVersion(source);
            return patchVersion.isEmpty()
                    ? CdhVersionForStreaming.VERSION_7_0_2_MISSING_PATCH_VERSION
                    : patchVersion.get() >= 2 ? CdhVersionForStreaming.VERSION_7_0_2_X : CdhVersionForStreaming.VERSION_7_0_2;
        } else {
            return CdhVersionForStreaming.VERSION_7_0_0;
        }
    }

    static String getCdhVersion(TemplatePreparationObject source) {
        return source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
    }

    @VisibleForTesting
    static Optional<Integer> getCdhPatchVersion(TemplatePreparationObject source) {
        if (null != source.getProductDetailsView() && null != source.getProductDetailsView().getProducts()) {
            Optional<ClouderaManagerProduct> cdh = source.getProductDetailsView().getProducts()
                    .stream()
                    .filter(p -> "CDH".equals(p.getName()))
                    .findAny();
            return cdh.flatMap(p -> getPatchFromVersionString(p.getVersion()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * @param version - the cdh version. Example expected format: 7.0.2-1.cdh7.0.2.p2.1672317
     * @return the patch version extracted
     */
    private static Optional<Integer> getPatchFromVersionString(String version) {
        Matcher matcher = Pattern.compile("\\.p([0-9]+)\\.").matcher(version);
        return matcher.find() ? Optional.of(Integer.valueOf(matcher.group(1))) : Optional.empty();
    }

    public enum CdhVersionForStreaming {
        VERSION_7_0_0(KafkaAuthConfigType.NO_AUTH),
        VERSION_7_0_2(KafkaAuthConfigType.LDAP_AUTH),
        VERSION_7_0_2_MISSING_PATCH_VERSION(KafkaAuthConfigType.LDAP_BASE_CONFIG),
        VERSION_7_0_2_X(KafkaAuthConfigType.SASL_LDAP_AUTH),
        VERSION_7_0_X(KafkaAuthConfigType.LDAP_AUTH),
        VERSION_7_1_0(KafkaAuthConfigType.SASL_LDAP_AUTH),
        VERSION_7_X_X(KafkaAuthConfigType.SASL_LDAP_AUTH);

        private static final EnumSet<CdhVersionForStreaming> SUPPORTS_RANGER_SERVICE_CREATION = EnumSet.of(
                VERSION_7_0_2_X, VERSION_7_1_0, VERSION_7_X_X);

        private static final EnumSet<CdhVersionForStreaming> NEEDS_ZK_SLL_WORKAROUND = EnumSet.of(VERSION_7_1_0);

        private static final EnumSet<CdhVersionForStreaming> SUPPORTS_CLOUD_JAR_STORAGE = EnumSet.of(VERSION_7_X_X);

        private final KafkaAuthConfigType authType;

        CdhVersionForStreaming(KafkaAuthConfigType authType) {
            this.authType = authType;
        }

        public boolean supportsRangerServiceCreation() {
            return SUPPORTS_RANGER_SERVICE_CREATION.contains(this);
        }

        public boolean needsZkSslWorkaround() {
            return NEEDS_ZK_SLL_WORKAROUND.contains(this);
        }

        public boolean supportsCloudJarStorage() {
            return SUPPORTS_CLOUD_JAR_STORAGE.contains(this);
        }

        KafkaAuthConfigType authType() {
            return authType;
        }
    }

    public enum KafkaAuthConfigType {
        NO_AUTH,
        LDAP_BASE_CONFIG,
        LDAP_AUTH,
        SASL_LDAP_AUTH
    }

}
