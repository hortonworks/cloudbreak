package com.sequenceiq.cloudbreak.cmtemplate.configproviders.flink;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.flink.FlinkConstants.FLINK;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

@Component
public class FlinkConfigProviderUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FlinkConfigProviderUtils.class);

    private static final String RELEASE_NAME_CONF_NAME = "release.name";

    public Optional<ClouderaManagerProduct> getFlinkProduct(List<ClouderaManagerProduct> products) {
        Optional<ClouderaManagerProduct> flinkProductOptional = products
                .stream()
                .filter(e -> e.getName().equalsIgnoreCase(FLINK))
                .findFirst();
        if (flinkProductOptional.isEmpty()) {
            LOG.warn("FLINK product not found!");
        }
        return flinkProductOptional;
    }

    public void addReleaseNameIfNeeded(
            String cdhVersion,
            List<ApiClusterTemplateConfig> configList,
            Optional<ClouderaManagerProduct> flinkProduct) {
        flinkProduct.ifPresent(fp -> {
            if (isUnifiedFlinkVersion(fp) && isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERA_STACK_VERSION_7_3_1)) {
                configList.add(config(RELEASE_NAME_CONF_NAME, "CSA-DH"));
            }
        });
    }

    static boolean isUnifiedFlinkVersion(ClouderaManagerProduct flinkProduct) {
        // Unified version scheme: 1.19.1-csa1.14.0.0-12345678
        return !StringUtils.containsIgnoreCase(flinkProduct.getVersion(), "csadh");
    }
}
