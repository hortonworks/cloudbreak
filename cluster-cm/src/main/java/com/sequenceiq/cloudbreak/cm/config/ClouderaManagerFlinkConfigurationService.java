package com.sequenceiq.cloudbreak.cm.config;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionEqualToLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.flink.FlinkConfigProviderUtils.RELEASE_NAME_CONF_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.flink.FlinkConfigProviderUtils.RELEASE_NAME_CONF_VALUE;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerConfigService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.util.VersionNormalizer;

@Component
public class ClouderaManagerFlinkConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerFlinkConfigurationService.class);

    private static final String FLINK_SERVICE_TYPE = "SQL_STREAM_BUILDER";

    @Inject
    private CmTemplateService cmTemplateService;

    @Inject
    private ClouderaManagerConfigService clouderaManagerConfigService;

    @Inject
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    public void addServiceConfigurationIfNecessary(ApiClient apiClient, StackDtoDelegate stackDto, Set<ClouderaManagerProduct> products) {
        if (isConfigNecessaryByRuntime(products) && isFLinkServicePresent(stackDto)) {
            try {
                LOGGER.debug("Trying to set the {} config to {} for Flink service.", RELEASE_NAME_CONF_NAME, RELEASE_NAME_CONF_VALUE);
                clouderaManagerConfigService.modifyServiceConfig(apiClient, stackDto.getName(), FLINK_SERVICE_TYPE,
                        Map.of(RELEASE_NAME_CONF_NAME, RELEASE_NAME_CONF_VALUE));
            } catch (CloudbreakException e) {
                String message = String.format("Failed to set the %s config to %s for Flink service.", RELEASE_NAME_CONF_NAME, RELEASE_NAME_CONF_VALUE);
                LOGGER.error(message, e);
                throw new CloudbreakServiceException(message, e);
            }
        }
    }

    private boolean isConfigNecessaryByRuntime(Set<ClouderaManagerProduct> products) {
        Optional<ClouderaManagerProduct> cdhProduct = clouderaManagerProductsProvider.getCdhProduct(products);
        return cdhProduct
                .map(cdh -> isVersionEqualToLimited(VersionNormalizer.normalizeCdhVersion(cdh.getVersion()), CLOUDERA_STACK_VERSION_7_3_1))
                .orElse(false);
    }

    private boolean isFLinkServicePresent(StackDtoDelegate stack) {
        return cmTemplateService.isServiceTypePresent(FLINK_SERVICE_TYPE, stack.getBlueprintJsonText());
    }
}
