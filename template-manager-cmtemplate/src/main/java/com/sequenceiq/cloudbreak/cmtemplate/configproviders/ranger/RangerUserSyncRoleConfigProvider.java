package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_8;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_18;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getCmVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class RangerUserSyncRoleConfigProvider extends AbstractRoleConfigProvider {
    public static final String LDAPUSERSYNC_PROCESS_LDAP_USER_GROUP_BUILDER = "org.apache.ranger.ldapusersync.process.LdapUserGroupBuilder";

    private static final String ROLE_ASSIGNMENT_RULES = "ranger.usersync.group.based.role.assignment.rules";

    private static final String ROLE_SAFETY_VALVE = "conf/ranger-ugsync-site.xml_role_safety_valve";

    private static final String RANGER_USERSYNC_UNIX_BACKEND = "ranger.usersync.unix.backend";

    private static final String RANGER_USERSYNC_AZURE_MAPPING = "ranger_usersync_azure_user_mapping";

    @Inject
    private VirtualGroupService virtualGroupService;

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        String adminGroup = virtualGroupService.createOrGetVirtualGroup(source.getVirtualGroupRequest(), UmsVirtualGroupRight.RANGER_ADMIN);
        String rangerAdminAsSysAdminConfigProvider = rangerAdminAsSysAdminConfigProvider(source);
        List<ApiClusterTemplateConfig> configs = new ArrayList<>(
                List.of(config(ROLE_SAFETY_VALVE, ConfigUtils.getSafetyValveProperty(RANGER_USERSYNC_UNIX_BACKEND, "nss")),
                        config(ROLE_ASSIGNMENT_RULES, "&ROLE_SYS_ADMIN:g:" + adminGroup + rangerAdminAsSysAdminConfigProvider)));
        if (CloudPlatform.AZURE.equals(source.getCloudPlatform()) && source.getGeneralClusterConfigs().isEnableRangerRaz()
                && source.getServicePrincipals() != null) {
            String servicePrincipals = getServicePrincipalsString(source);
            configs.add(config(RANGER_USERSYNC_AZURE_MAPPING, servicePrincipals));
        }
        configs.addAll(createUsersyncLdapConfig(source));
        return configs;
    }

    private List<ApiClusterTemplateConfig> createUsersyncLdapConfig(TemplatePreparationObject source) {
        if (source.getLdapConfig().isPresent()
                && isVersionNewerOrEqualThanLimited(source.getBlueprintView().getProcessor().getStackVersion(), CLOUDERA_STACK_VERSION_7_2_18)
                && entitlementService.isRangerLdapUsersyncEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            LdapView ldapView = source.getLdapConfig().get();
            return List.of(
                    config("ranger.usersync.ldap.url", ldapView.getConnectionURL()),
                    config("ranger.usersync.ldap.binddn", ldapView.getBindDn()),
                    config("ranger_usersync_ldap_ldapbindpassword", ldapView.getBindPassword()),
                    config("ranger.usersync.ldap.user.searchbase", ldapView.getUserSearchBase()),
                    config("ranger.usersync.ldap.user.objectclass", ldapView.getUserObjectClass()),
                    config("ranger.usersync.ldap.user.nameattribute", ldapView.getUserNameAttribute()),
                    config("ranger.usersync.group.objectclass", ldapView.getGroupObjectClass()),
                    config("ranger.usersync.group.nameattribute", ldapView.getGroupNameAttribute()),
                    config("ranger.usersync.group.memberattributename", ldapView.getGroupMemberAttribute()),
                    config("ranger.usersync.group.searchbase", ldapView.getGroupSearchBase()),
                    config("ranger.usersync.source.impl.class", LDAPUSERSYNC_PROCESS_LDAP_USER_GROUP_BUILDER),
                    config("ranger.usersync.ldap.user.searchfilter", "uid=*"),
                    config("ranger.usersync.group.searchfilter", "cn=*"),
                    config(ROLE_SAFETY_VALVE, ConfigUtils.getSafetyValveProperty("ranger.usersync.cdp.public", Boolean.TRUE.toString())),
                    config(ROLE_SAFETY_VALVE, ConfigUtils.getSafetyValveProperty("ranger.usersync.syncsource.validation.enabled", Boolean.FALSE.toString())));
        } else {
            return List.of();
        }
    }

    private String rangerAdminAsSysAdminConfigProvider(TemplatePreparationObject source) {
        String cmVersion = getCmVersion(source);
        if (isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_2_8)) {
            return "&ROLE_SYS_ADMIN:u:ranger";
        }
        return "";
    }

    private String getServicePrincipalsString(TemplatePreparationObject source) {
        if (source.getServicePrincipals().isEmpty()) {
            return "";
        } else {
            return source.getServicePrincipals().entrySet().stream()
                    .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                    .collect(Collectors.joining(";"));
        }
    }

    @Override
    public String getServiceType() {
        return RangerRoles.RANGER;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(RangerRoles.RANGER_USERSYNC);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices())
                && source.getGatewayView().getExposedServices().contains(exposedServiceCollector.getRangerService().getKnoxService());
    }
}
