package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CFM_VERSION_2_0_0_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CFM_VERSION_2_2_3_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionOlderThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.cfm.CfmUtil.getCfmProduct;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
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

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        LOGGER.info("add property values for NifiKnoxRoleConfigProvider");
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();

        Set<String> topologyNames = source.getGatewayView().getGatewayTopologies().keySet();
        if (!topologyNames.isEmpty()) {
            String clusterName = source.getGeneralClusterConfigs().getClusterName();
            String proxyContextPath = topologyNames.stream()
                    .map(topologyName -> String.format("%s/%s/nifi-app,%s/%s/nifi-app",
                            clusterName, topologyName,
                            clusterName, topologyName + "-api"))
                    .collect(Collectors.joining(","));
            LOGGER.debug("{} = {} added to template", PROXY_CONTEXT_PATH, proxyContextPath);
            configs.add(config(PROXY_CONTEXT_PATH, proxyContextPath));
        }

        Optional<ClouderaManagerProduct> cfm = getCfmProduct(source);
        Optional<ClusterExposedServiceView> nifi = getNifi(source);

        if (cfm.isPresent() && isVersionNewerOrEqualThanLimited(cfm.get().getVersion(), CFM_VERSION_2_0_0_0) && nifi.isPresent()) {
            String nifiUiKnoxURL = nifi.get().getServiceUrl();
            LOGGER.debug("{} = {} added to template", NIFI_UI_KNOX_URL, nifiUiKnoxURL);
            configs.add(config(NIFI_UI_KNOX_URL, nifiUiKnoxURL));
        }
        return configs;
    }

    private Optional<ClusterExposedServiceView> getNifi(TemplatePreparationObject source) {
        if (source.getExposedServices() != null && !source.getExposedServices().isEmpty()) {
            Optional<ClusterExposedServiceView> nifi = source.getExposedServices().get("cdp-proxy")
                    .stream()
                    .filter(e -> e.getKnoxService().equalsIgnoreCase(exposedServiceCollector.getNiFiService().getName()))
                    .findFirst();
            if (nifi.isEmpty()) {
                LOGGER.info("Nifi is not presented as an Exposed service");
            }
            return nifi;
        }
        LOGGER.info("Exposed service null or empty for NifiKnoxRoleConfigProvider");
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
        Optional<ClouderaManagerProduct> cfm = getCfmProduct(source);
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices())
                && source.getGatewayView().getExposedServices().contains(exposedServiceCollector.getNiFiService().getKnoxService())
                && cfm.isPresent()
                && isVersionOlderThanLimited(cfm.get().getVersion(), CFM_VERSION_2_2_3_0);
    }

}

