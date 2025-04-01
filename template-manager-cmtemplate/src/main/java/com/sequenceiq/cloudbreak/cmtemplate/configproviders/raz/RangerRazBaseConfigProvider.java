package com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isRazConfigurationForRazRoleNeeded;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isRazConfigurationForServiceTypeSupported;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz.RangerRazRoles.RANGER_RAZ;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz.RangerRazRoles.RANGER_RAZ_SERVER;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.identitymapping.AccountMappingSubject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.AccountMappingView;

public abstract class RangerRazBaseConfigProvider extends AbstractRoleConfigProvider {

    private static final String RANGER_RAZ_SITE_XML_ROLE_SAFETY_VALVE = "ranger-raz-conf/ranger-raz-site.xml_role_safety_valve";

    private static final String RANGER_RAZ_BOOTSTRAP_SERVICETYPES = "ranger.raz.bootstrap.servicetypes";

    private static final String RANGER_RAZ_GCP_SERVICE_ACCOUNT = "ranger.raz.gs.service.account";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return List.of();
    }

    @Override
    public String getServiceType() {
        return RANGER_RAZ;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(RANGER_RAZ_SERVER);
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> roleConfigs = new ArrayList<>();
        String cdhVersion = ConfigUtils.getCdhVersion(source);
        CloudPlatform cloudPlatform = source.getCloudPlatform();
        StringBuffer safetyValveValue = new StringBuffer();
        if (!Strings.isNullOrEmpty(cdhVersion) && isRazConfigurationForServiceTypeSupported(cdhVersion, cloudPlatform, source.getStackType())) {
            safetyValveValue.append(getSafetyValveProperty(RANGER_RAZ_BOOTSTRAP_SERVICETYPES, getServiceType(cloudPlatform)));
        }
        if (isRazConfigurationForRazRoleNeeded(source.getProductDetailsView().getCm().getVersion(), cloudPlatform, source.getStackType())) {
            String rangerCloudAccessAuthorizerServiceAccount = getRangerCloudAccessAuthorizerServiceAccount(source);
            if (rangerCloudAccessAuthorizerServiceAccount != null) {
                safetyValveValue.append(getSafetyValveProperty(RANGER_RAZ_GCP_SERVICE_ACCOUNT, rangerCloudAccessAuthorizerServiceAccount));
            }
        }
        if (!Strings.isNullOrEmpty(safetyValveValue.toString())) {
            roleConfigs.add(config(RANGER_RAZ_SITE_XML_ROLE_SAFETY_VALVE, safetyValveValue.toString()));
        }
        return roleConfigs;
    }

    private String getServiceType(CloudPlatform cloudPlatform) {
        switch (cloudPlatform) {
            case AZURE:
                return "adls";
            case AWS:
                return "s3";
            case GCP:
                return "gs";
            default:
                return null;
        }
    }

    private String getRangerCloudAccessAuthorizerServiceAccount(TemplatePreparationObject source) {
        AccountMappingView accountMappingView = source.getAccountMappingView() == null
                ? AccountMappingView.EMPTY_MAPPING : source.getAccountMappingView();
        Map<String, String> userMappings = accountMappingView.getUserMappings();
        return userMappings == null ? null : userMappings.get(AccountMappingSubject.RANGER_RAZ_USER);
    }

    protected ApiClusterTemplateService createTemplate() {
        ApiClusterTemplateService coreSettings = new ApiClusterTemplateService()
                .serviceType(RANGER_RAZ)
                .refName("ranger-RANGER_RAZ");
        ApiClusterTemplateRoleConfigGroup coreSettingsRole = new ApiClusterTemplateRoleConfigGroup()
                .roleType(RANGER_RAZ_SERVER)
                .base(true)
                .refName("ranger-RANGER_RAZ_SERVER");
        coreSettings.roleConfigGroups(List.of(coreSettingsRole));
        return coreSettings;
    }
}
