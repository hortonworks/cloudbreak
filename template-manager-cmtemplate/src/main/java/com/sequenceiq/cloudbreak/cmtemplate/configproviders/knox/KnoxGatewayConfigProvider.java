package com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.GatewayView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class KnoxGatewayConfigProvider extends AbstractRoleConfigProvider {

    private static final String KNOX_SERVICE_REF_NAME = "knox";

    private static final String KNOX_GATEWAY_REF_NAME = "knox-KNOX_GATEWAY-BASE";

    private static final String KNOX_MASTER_SECRET = "gateway_master_secret";

    private static final String GATEWAY_PATH = "gateway_path";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        GatewayView gateway = source.getGatewayView();
        String masterSecret = gateway != null ? gateway.getMasterSecret() : source.getGeneralClusterConfigs().getPassword();

        switch (roleType) {
            case KnoxRoles.KNOX_GATEWAY:
                List<ApiClusterTemplateConfig> config = new ArrayList<>();
                config.add(config(KNOX_MASTER_SECRET, masterSecret));
                if (gateway != null) {
                    config.add(config(GATEWAY_PATH, gateway.getPath()));
                }
                return config;
            case KnoxRoles.IDBROKER:
                return List.of(config("idbroker_master_secret", masterSecret));
            default:
                return List.of();
        }
    }

    @Override
    public Map<String, ApiClusterTemplateService> getAdditionalServices(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        if (source.getGatewayView() != null && cmTemplateProcessor.getServiceByType(KnoxRoles.KNOX).isEmpty()) {
            ApiClusterTemplateService knox = createBaseKnoxService();
            Set<HostgroupView> hostgroupViews = source.getHostgroupViews();
            return hostgroupViews.stream()
                    .filter(hg -> InstanceGroupType.GATEWAY.equals(hg.getInstanceGroupType()))
                    .collect(Collectors.toMap(HostgroupView::getName, v -> knox));
        }
        return Map.of();
    }

    private ApiClusterTemplateService createBaseKnoxService() {
        ApiClusterTemplateService knox = new ApiClusterTemplateService().serviceType(KnoxRoles.KNOX).refName(KNOX_SERVICE_REF_NAME);
        ApiClusterTemplateRoleConfigGroup knoxGateway = new ApiClusterTemplateRoleConfigGroup()
                .roleType(KnoxRoles.KNOX_GATEWAY).base(true).refName(KNOX_GATEWAY_REF_NAME);
        knox.roleConfigGroups(List.of(knoxGateway));
        return knox;
    }

    @Override
    public String getServiceType() {
        return KnoxRoles.KNOX;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(KnoxRoles.KNOX_GATEWAY, KnoxRoles.IDBROKER);
    }
}
