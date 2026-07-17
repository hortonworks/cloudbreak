package com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isGcpRazCabAuthTypeSupported;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.constant.GcpConstants;
import com.sequenceiq.cloudbreak.service.identitymapping.AccountMappingSubject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;

public abstract class RangerRazBaseConfigProvider extends AbstractRoleConfigProvider {

    private static final String RANGER_RAZ_SITE_XML_ROLE_SAFETY_VALVE = "ranger-raz-conf/ranger-raz-site.xml_role_safety_valve";

    private static final String RANGER_RAZ_BOOTSTRAP_SERVICETYPES = "ranger.raz.bootstrap.servicetypes";

    private static final String RANGER_RAZ_GCP_SERVICE_ACCOUNT = "ranger.raz.gs.service.account";

    private static final String RANGER_RAZ_GS_AUTH_TYPE = "ranger_raz_gs_auth_type";

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
        StringBuilder safetyValveValue = new StringBuilder();
        if (!Strings.isNullOrEmpty(cdhVersion) && isRazConfigurationForServiceTypeSupported(cdhVersion, cloudPlatform, source.getStackType())) {
            safetyValveValue.append(getSafetyValveProperty(RANGER_RAZ_BOOTSTRAP_SERVICETYPES, getServiceType(cloudPlatform)));
        }
        if (isRazConfigurationForRazRoleNeeded(source.getProductDetailsView().getCm().getVersion(), cloudPlatform, source.getStackType())) {
            String rangerCloudAccessAuthorizerServiceAccount = getRangerCloudAccessAuthorizerServiceAccount(source);
            if (rangerCloudAccessAuthorizerServiceAccount != null) {
                safetyValveValue.append(getSafetyValveProperty(RANGER_RAZ_GCP_SERVICE_ACCOUNT, rangerCloudAccessAuthorizerServiceAccount));
            }
            if (isGcpRazCabAuthTypeSupported(source.getProductDetailsView().getCm().getVersion())
                    && GcpConstants.RAZ_AUTHENTICATION_TYPE_CAB.equals(getRazAuthenticationType(source))) {
                roleConfigs.add(config(RANGER_RAZ_GS_AUTH_TYPE, GcpConstants.RAZ_AUTHENTICATION_TYPE_CAB));
            }
        }
        if (!Strings.isNullOrEmpty(safetyValveValue.toString())) {
            roleConfigs.add(config(RANGER_RAZ_SITE_XML_ROLE_SAFETY_VALVE, safetyValveValue.toString()));
        }
        return roleConfigs;
    }

    private String getRazAuthenticationType(TemplatePreparationObject source) {
        if (StackType.WORKLOAD == source.getStackType()) {
            return source.getDatalakeView().map(DatalakeView::getRazAuthenticationType).orElse(null);
        } else if (StackType.DATALAKE == source.getStackType()) {
            return source.getGeneralClusterConfigs().getRazAuthenticationType();
        } else {
            return null;
        }
    }

    private String getServiceType(CloudPlatform cloudPlatform) {
        return switch (cloudPlatform) {
            case AZURE -> "adls";
            case AWS -> "s3";
            case GCP -> "gs";
            default -> null;
        };
    }

    private String getRangerCloudAccessAuthorizerServiceAccount(TemplatePreparationObject source) {
        Map<String, String> userMappings;
        if (source.getStackType() == StackType.WORKLOAD) {
            userMappings = source.getDatalakeView().isPresent() ? source.getDatalakeView().get().getUserMappings() : Map.of();
        } else {
            userMappings = source.getAccountMappingView() == null ? Map.of() : source.getAccountMappingView().getUserMappings();
        }
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
