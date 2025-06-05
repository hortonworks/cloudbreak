package com.sequenceiq.cloudbreak.cmtemplate.configproviders.rms;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_18;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isHmsRangerServiceNameRequired;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.rms.RangerRmsRoles.RANGER_RMS_SERVER_REF_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.rms.RangerRmsRoles.RANGER_RMS_SERVER_ROLE_TYPE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.rms.RangerRmsRoles.RANGER_RMS_SERVICE_REF_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.rms.RangerRmsRoles.RANGER_RMS_SERVICE_TYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupName;

@Component
public class RangerRmsDatalakeConfigProvider extends AbstractRoleConfigProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RangerRmsDatalakeConfigProvider.class);

    private static final Set<String> ADDITIONAL_SERVICE_HOSTGROUP = Set.of(InstanceGroupName.MASTER.getName());

    private static final String HMS_MAP_MANAGED_TABLES = "ranger_rms_hms_map_managed_tables";

    private static final String HMS_SOURCE_SERVICE_NAME = "ranger_rms_hms_source_service_name";

    private static final String HMS_SOURCE_SERVICE_NAME_VALUE = "cm_s3";

    private static final String SUPPORTED_URI_SCHEME = "ranger_rms_supported_uri_scheme";

    private static final String SUPPORTED_URI_SCHEME_VALUE = "s3a";

    private static final String HA_ENABLED = "ranger_rms_server_ha_enabled";

    private static final String SERVER_IDS = "ranger_rms_server_ids";

    private static final String SERVER_IDS_VALUE = "id1,id2";

    @Inject
    private EntitlementService entitlementService;

    @Override
    public String getServiceType() {
        return RANGER_RMS_SERVICE_TYPE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(RANGER_RMS_SERVER_ROLE_TYPE);
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        configs.add(config(HMS_MAP_MANAGED_TABLES, Boolean.TRUE.toString()));
        if (isHmsRangerServiceNameRequired(source.getProductDetailsView().getCm().getVersion())) {
            configs.add(config(HMS_SOURCE_SERVICE_NAME, HMS_SOURCE_SERVICE_NAME_VALUE));
        }
        configs.add(config(SUPPORTED_URI_SCHEME, SUPPORTED_URI_SCHEME_VALUE));
        if (isHa(source)) {
            configs.add(config(HA_ENABLED, Boolean.TRUE.toString()));
            configs.add(config(SERVER_IDS, SERVER_IDS_VALUE));
        }
        return configs;
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return entitlementService.isRmsEnabledOnDatalake(ThreadBasedUserCrnProvider.getAccountId())
                && StackType.DATALAKE == source.getStackType()
                && CloudPlatform.AWS == source.getCloudPlatform()
                && source.getGeneralClusterConfigs().isEnableRangerRaz()
                && source.getGeneralClusterConfigs().isEnableRangerRms()
                && isVersionSupported(cmTemplateProcessor, source)
                && CMRepositoryVersionUtil.isRazConfigurationSupported(
                source.getProductDetailsView().getCm().getVersion(), source.getCloudPlatform(), source.getStackType());
    }

    @Override
    public Map<String, ApiClusterTemplateService> getAdditionalServices(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        if (isConfigurationNeeded(cmTemplateProcessor, source)) {
            ApiClusterTemplateService coreSettings = getRangerRmsTemplate();
            Set<HostgroupView> hostgroupViews = source.getHostgroupViews();
            return hostgroupViews.stream()
                    .filter(hg -> ADDITIONAL_SERVICE_HOSTGROUP.contains(hg.getName().toLowerCase()))
                    .collect(Collectors.toMap(HostgroupView::getName, v -> coreSettings));
        }
        return Map.of();
    }

    public Set<String> getHostGroups(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return getAdditionalServices(cmTemplateProcessor, source).keySet();
    }

    private ApiClusterTemplateService getRangerRmsTemplate() {
        ApiClusterTemplateService clusterTemplateService = new ApiClusterTemplateService()
                .serviceType(RANGER_RMS_SERVICE_TYPE)
                .refName(RANGER_RMS_SERVICE_REF_NAME);
        ApiClusterTemplateRoleConfigGroup clusterTemplateRoleConfigGroup = new ApiClusterTemplateRoleConfigGroup()
                .roleType(RANGER_RMS_SERVER_ROLE_TYPE)
                .base(true)
                .refName(RANGER_RMS_SERVER_REF_NAME);
        clusterTemplateService.addRoleConfigGroupsItem(clusterTemplateRoleConfigGroup);
        return clusterTemplateService;
    }

    private boolean isVersionSupported(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        if (cmTemplateProcessor.getVersion().isPresent()) {
            return isVersionNewerOrEqualThanLimited(cmTemplateProcessor.getVersion().get(), CLOUDERA_STACK_VERSION_7_2_18);
        }
        String version = source.getBlueprintView().getVersion();
        if (StringUtils.isNotBlank(version)) {
            return isVersionNewerOrEqualThanLimited(version, CLOUDERA_STACK_VERSION_7_2_18);
        }
        LOG.warn("Could not determine the version of the source. Source: {}", source);
        return false;
    }

    private boolean isHa(TemplatePreparationObject source) {
        return source.getHostgroupViews()
                .stream()
                .filter(hg -> InstanceGroupName.MASTER.getName().equals(hg.getName().toLowerCase(Locale.ROOT)))
                .findFirst()
                .filter(hostgroupView -> hostgroupView.getNodeCount() > 1)
                .isPresent();
    }
}
