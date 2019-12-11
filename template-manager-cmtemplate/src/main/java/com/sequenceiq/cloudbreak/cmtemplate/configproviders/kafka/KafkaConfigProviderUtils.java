package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_2;

public class KafkaConfigProviderUtils {

    private KafkaConfigProviderUtils() {
    }

    static CdhVersionForStreaming getCdhVersionForStreaming(TemplatePreparationObject source) {
        String cdhVersion = source.getBlueprintView().getProcessor().getVersion().orElse("");
        Comparator<Versioned> versionComparator = new VersionComparator();
        int compareResult = versionComparator.compare(() -> cdhVersion, CLOUDERAMANAGER_VERSION_7_0_2);
        if (compareResult < 0) {
            return CdhVersionForStreaming.VERSION_PRE_7_0_2;
        } else if (compareResult > 0) {
            return CdhVersionForStreaming.VERSION_7_0_2_2_OR_LATER;
        } else {
            Optional<Integer> patchVersion = getCdhPatchVersion(source);
            return patchVersion.isEmpty()
                    ? CdhVersionForStreaming.VERSION_7_0_2_CANNOT_DETERMINE_PATCH
                    : patchVersion.get() >= 2 ? CdhVersionForStreaming.VERSION_7_0_2_2_OR_LATER : CdhVersionForStreaming.VERSION_7_0_2;
        }
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

    enum CdhVersionForStreaming {
        VERSION_PRE_7_0_2,
        VERSION_7_0_2,
        VERSION_7_0_2_CANNOT_DETERMINE_PATCH,
        VERSION_7_0_2_2_OR_LATER
    }

}
