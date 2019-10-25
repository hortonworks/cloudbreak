package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CFM_VERSION_2_0_0_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    private static final String PROXY_CONTEXT_PATH = "nifi.web.proxy.context.path";

    private static final String NIFI_UI_KNOX_URL = "nifi.ui.knox.url";

    private static final String CFM = "CFM";

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        String clusterName = source.getGeneralClusterConfigs().getClusterName();
        String topologyName = source.getGatewayView().getTopologyName();
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();

        configs.add(config(PROXY_CONTEXT_PATH, String.format("%s/%s/nifi-app", clusterName, topologyName)));

        Optional<ClouderaManagerProduct> cfm = getClouderaManagerProduct(source);
        Optional<ClusterExposedServiceView> nifi = getNifi(source);

        if (cfm.isPresent() && isVersionNewerOrEqualThanLimited(cfm.get().getVersion(), CFM_VERSION_2_0_0_0)
            && nifi.isPresent()) {
            configs.add(config(NIFI_UI_KNOX_URL, nifi.get().getServiceUrl()));
        }
        return configs;
    }

    private Optional<ClusterExposedServiceView> getNifi(TemplatePreparationObject source) {
        if (source.getExposedServices() != null && !source.getExposedServices().isEmpty()) {
            return source.getExposedServices().get("cdp-proxy")
                    .stream()
                    .filter(e -> e.getKnoxService().equalsIgnoreCase(ExposedService.NIFI.name()))
                    .findFirst();
        }
        return Optional.empty();
    }

    private Optional<ClouderaManagerProduct> getClouderaManagerProduct(TemplatePreparationObject source) {
        if (source.getProductDetailsView() != null) {
            source.getProductDetailsView().getProducts()
                    .stream()
                    .filter(e -> e.getName().equalsIgnoreCase(CFM))
                    .findFirst();
        }
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
