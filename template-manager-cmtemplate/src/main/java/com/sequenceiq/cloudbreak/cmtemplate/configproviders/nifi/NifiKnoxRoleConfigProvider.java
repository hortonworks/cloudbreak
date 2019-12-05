package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CFM_VERSION_2_0_0_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.ClusterExposedServiceView;

@Component
public class NifiKnoxRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NifiKnoxRoleConfigProvider.class);

    private static final String PROXY_CONTEXT_PATH = "nifi.web.proxy.context.path";

    private static final String NIFI_UI_KNOX_URL = "nifi.ui.knox.url";

    private static final String CFM = "CFM";

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        LOGGER.info("add property values for NifiKnoxRoleConfigProvider");
        String clusterName = source.getGeneralClusterConfigs().getClusterName();
        String topologyName = source.getGatewayView().getTopologyName();
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();

        configs.add(config(PROXY_CONTEXT_PATH, String.format("%s/%s/nifi-app", clusterName, topologyName)));
        LOGGER.debug("PROXY_CONTEXT_PATH added to template");

        Optional<ClouderaManagerProduct> cfm = getClouderaManagerProduct(source);
        Optional<ClusterExposedServiceView> nifi = getNifi(source);

        if (cfm.isPresent() && isVersionNewerOrEqualThanLimited(cfm.get().getVersion(), CFM_VERSION_2_0_0_0) && nifi.isPresent()) {
            LOGGER.debug("NIFI_UI_KNOX_URL added to template");
            configs.add(config(NIFI_UI_KNOX_URL, nifi.get().getServiceUrl()));
        }
        return configs;
    }

    private Optional<ClusterExposedServiceView> getNifi(TemplatePreparationObject source) {
        if (source.getExposedServices() != null && !source.getExposedServices().isEmpty()) {
            Optional<ClusterExposedServiceView> nifi = source.getExposedServices().get("cdp-proxy")
                    .stream()
                    .filter(e -> e.getKnoxService().equalsIgnoreCase(ExposedService.NIFI.name()))
                    .findFirst();
            if (nifi.isEmpty()) {
                LOGGER.info("Nifi is not presented as an Exposed service");
            }
            return nifi;
        }
        LOGGER.info("Exposed service null or empty for NifiKnoxRoleConfigProvider");
        return Optional.empty();
    }

    private Optional<ClouderaManagerProduct> getClouderaManagerProduct(TemplatePreparationObject source) {
        if (source.getProductDetailsView() != null) {
            Optional<ClouderaManagerProduct> cfm = source.getProductDetailsView().getProducts()
                    .stream()
                    .filter(e -> e.getName().equalsIgnoreCase(CFM))
                    .findFirst();
            if (cfm.isEmpty()) {
                LOGGER.info("CFM is not presented as a Production");
            }
            return cfm;
        }
        LOGGER.info("Product null for NifiKnoxRoleConfigProvider");
        return Optional.empty();
    }

    @Override
    public String getServiceType() {
        return "NIFI";
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of("NIFI_NODE");
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices())
                && source.getGatewayView().getExposedServices().contains(ExposedService.NIFI.getKnoxService());
    }

}
