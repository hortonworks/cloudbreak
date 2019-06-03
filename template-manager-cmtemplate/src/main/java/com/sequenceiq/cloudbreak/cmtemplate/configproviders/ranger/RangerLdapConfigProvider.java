package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.configVar;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.variable;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.dto.LdapView;

@Component
public class RangerLdapConfigProvider extends AbstractRoleConfigConfigProvider {

    private static final String RANGER_AUTHENTICATION_METHOD = "ranger.authentication.method";

    private static final String RANGER_LDAP_AD_DOMAIN = "ranger.ldap.ad.domain";

    private static final String RANGER_LDAP_AD_URL = "ranger.ldap.ad.url";

    private static final String RANGER_LDAP_AD_BIND_DN = "ranger.ldap.ad.bind.dn";

    private static final String RANGER_LDAP_AD_BIND_PASSWORD = "ranger_ldap_ad_bind_password";

    private static final String RANGER_LDAP_AD_BASE_DN = "ranger.ldap.ad.base.dn";

    private static final String RANGER_LDAP_AD_USER_SEARCHFILTER = "ranger.ldap.ad.user.searchfilter";

    private static final String RANGER_LDAP_URL = "ranger.ldap.url";

    private static final String RANGER_LDAP_BIND_DN = "ranger.ldap.bind.dn";

    private static final String RANGER_LDAP_BIND_PASSWORD = "ranger_ldap_bind_password";

    private static final String RANGER_LDAP_BASE_DN = "ranger.ldap.base.dn";

    private static final String RANGER_LDAP_USER_SEARCHFILTER = "ranger.ldap.user.searchfilter";

    private static final String RANGER_LDAP_USER_DNPATTERN = "ranger.ldap.user.dnpattern";

    private static final String RANGER_LDAP_GROUP_SEARCHBASE = "ranger.ldap.group.searchbase";

    private static final String RANGER_LDAP_GROUP_ROLEATTRIBUTE = "ranger.ldap.group.roleattribute";

    private static final String RANGER_USERSYNC_LDAP_URL = "ranger.usersync.ldap.url";

    private static final String RANGER_USERSYNC_LDAP_BINDDN = "ranger.usersync.ldap.binddn";

    private static final String RANGER_USERSYNC_LDAP_LDAPBINDPASSWORD = "ranger_usersync_ldap_ldapbindpassword";

    private static final String RANGER_USERSYNC_LDAP_USER_NAMEATTRIBUTE = "ranger.usersync.ldap.user.nameattribute";

    private static final String RANGER_USERSYNC_LDAP_USER_SEARCHBASE = "ranger.usersync.ldap.user.searchbase";

    private static final String RANGER_USERSYNC_LDAP_USER_OBJECTCLASS = "ranger.usersync.ldap.user.objectclass";

    private static final String RANGER_USERSYNC_GROUP_MEMBERATTRIBUTENAME = "ranger.usersync.group.memberattributename";

    private static final String RANGER_USERSYNC_GROUP_NAMEATTRIBUTE = "ranger.usersync.group.nameattribute";

    private static final String RANGER_USERSYNC_GROUP_OBJECTCLASS = "ranger.usersync.group.objectclass";

    private static final String RANGER_USERSYNC_GROUP_SEARCHBASE = "ranger.usersync.group.searchbase";

    private static final String RANGER_USERSYNC_GROUP_SEARCHFILTER = "ranger.usersync.group.searchfilter";

    private static final String RANGER_USERSYNC_GROUP_BASED_ROLE_ASSIGNMENT_RULES = "ranger.usersync.group.based.role.assignment.rules";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        String hostGroup = hostGroupView.getName();
        switch (roleType) {
            case RangerRoles.RANGER_ADMIN:
                return List.of(
                        configVar(RANGER_AUTHENTICATION_METHOD, getRoleTypeVariableName(hostGroup, roleType, RANGER_AUTHENTICATION_METHOD)),
                        configVar(RANGER_LDAP_AD_DOMAIN, getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_AD_DOMAIN)),
                        configVar(RANGER_LDAP_AD_URL, getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_AD_URL)),
                        configVar(RANGER_LDAP_AD_BIND_DN, getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_AD_BIND_DN)),
                        configVar(RANGER_LDAP_AD_BIND_PASSWORD, getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_AD_BIND_PASSWORD)),
                        configVar(RANGER_LDAP_AD_BASE_DN, getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_AD_BASE_DN)),
                        configVar(RANGER_LDAP_AD_USER_SEARCHFILTER, getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_AD_USER_SEARCHFILTER)),
                        configVar(RANGER_LDAP_URL, getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_URL)),
                        configVar(RANGER_LDAP_BIND_DN, getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_BIND_DN)),
                        configVar(RANGER_LDAP_BIND_PASSWORD, getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_BIND_PASSWORD)),
                        configVar(RANGER_LDAP_BASE_DN, getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_BASE_DN)),
                        configVar(RANGER_LDAP_USER_SEARCHFILTER, getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_USER_SEARCHFILTER)),
                        configVar(RANGER_LDAP_USER_DNPATTERN, getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_USER_DNPATTERN)),
                        configVar(RANGER_LDAP_GROUP_SEARCHBASE, getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_GROUP_SEARCHBASE)),
                        configVar(RANGER_LDAP_GROUP_ROLEATTRIBUTE, getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_GROUP_ROLEATTRIBUTE))
                );
                // Note: the following settings are not required:
                // ranger.ldap.group.searchfilter
            case RangerRoles.RANGER_USERSYNC:
                return List.of(
                        config("ranger.usersync.enabled", Boolean.TRUE.toString()),
                        config("ranger.usersync.source.impl.class", "org.apache.ranger.ldapusersync.process.LdapUserGroupBuilder"),
                        configVar(RANGER_USERSYNC_LDAP_URL, getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_LDAP_URL)),
                        configVar(RANGER_USERSYNC_LDAP_BINDDN, getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_LDAP_BINDDN)),
                        configVar(RANGER_USERSYNC_LDAP_LDAPBINDPASSWORD, getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_LDAP_LDAPBINDPASSWORD)),
                        configVar(RANGER_USERSYNC_LDAP_USER_NAMEATTRIBUTE,
                                getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_LDAP_USER_NAMEATTRIBUTE)),
                        configVar(RANGER_USERSYNC_LDAP_USER_SEARCHBASE, getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_LDAP_USER_SEARCHBASE)),
                        configVar(RANGER_USERSYNC_LDAP_USER_OBJECTCLASS, getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_LDAP_USER_OBJECTCLASS)),
                        config("ranger.usersync.ldap.deltasync", Boolean.FALSE.toString()),
                        config("ranger.usersync.group.searchenabled", Boolean.TRUE.toString()),
                        configVar(RANGER_USERSYNC_GROUP_MEMBERATTRIBUTENAME,
                                getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_GROUP_MEMBERATTRIBUTENAME)),
                        configVar(RANGER_USERSYNC_GROUP_NAMEATTRIBUTE, getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_GROUP_NAMEATTRIBUTE)),
                        configVar(RANGER_USERSYNC_GROUP_OBJECTCLASS, getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_GROUP_OBJECTCLASS)),
                        configVar(RANGER_USERSYNC_GROUP_SEARCHBASE, getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_GROUP_SEARCHBASE)),
                        configVar(RANGER_USERSYNC_GROUP_SEARCHFILTER, getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_GROUP_SEARCHFILTER)),
                        configVar(RANGER_USERSYNC_GROUP_BASED_ROLE_ASSIGNMENT_RULES,
                                getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_GROUP_BASED_ROLE_ASSIGNMENT_RULES))
                );
                // Note: the following settings are not required:
                // ranger.usersync.ldap.searchBase
                // ranger.usersync.ldap.user.searchfilter
                // ranger.usersync.ldap.user.nameattribute
                // ranger.usersync.ldap.user.groupnameattribute
            default:
                return List.of();
        }
    }

    @Override
    protected List<ApiClusterTemplateVariable> getRoleConfigVariable(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        String hostGroup = hostGroupView.getName();
        LdapView ldapView = source.getLdapConfig().get();
        switch (roleType) {
            case RangerRoles.RANGER_ADMIN:
                return List.of(
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_AUTHENTICATION_METHOD), ldapView.getDirectoryType().toString()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_AD_DOMAIN), ldapView.getDomain()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_AD_URL), ldapView.getConnectionURL()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_AD_BIND_DN), ldapView.getBindDn()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_AD_BIND_PASSWORD), ldapView.getBindPassword()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_AD_BASE_DN), ldapView.getUserSearchBase()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_AD_USER_SEARCHFILTER), "(cn={0})"),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_URL), ldapView.getConnectionURL()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_BIND_DN), ldapView.getBindDn()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_BIND_PASSWORD), ldapView.getBindPassword()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_BASE_DN), ldapView.getUserSearchBase()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_USER_SEARCHFILTER), "(cn={0})"),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_USER_DNPATTERN), ldapView.getUserDnPattern()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_GROUP_SEARCHBASE), ldapView.getGroupSearchBase()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_LDAP_GROUP_ROLEATTRIBUTE), ldapView.getGroupNameAttribute())
                );
                // Note: the original Ambari value for RANGER_LDAP_AD_DOMAIN was " "; that is certainly a mistake.
            case RangerRoles.RANGER_USERSYNC:
                return List.of(
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_LDAP_URL), ldapView.getConnectionURL()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_LDAP_BINDDN), ldapView.getBindDn()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_LDAP_LDAPBINDPASSWORD), ldapView.getBindPassword()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_LDAP_USER_NAMEATTRIBUTE), ldapView.getUserNameAttribute()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_LDAP_USER_SEARCHBASE), ldapView.getUserSearchBase()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_LDAP_USER_OBJECTCLASS), ldapView.getUserObjectClass()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_GROUP_MEMBERATTRIBUTENAME), ldapView.getGroupMemberAttribute()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_GROUP_NAMEATTRIBUTE), ldapView.getGroupNameAttribute()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_GROUP_OBJECTCLASS), ldapView.getGroupObjectClass()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_GROUP_SEARCHBASE), ldapView.getGroupSearchBase()),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_GROUP_SEARCHFILTER), " "),
                        variable(getRoleTypeVariableName(hostGroup, roleType, RANGER_USERSYNC_GROUP_BASED_ROLE_ASSIGNMENT_RULES),
                                String.format("&ROLE_SYS_ADMIN:g:%s", ldapView.getAdminGroup()))
                );
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return RangerRoles.RANGER;
    }

    @Override
    public List<String> getRoleTypes() {
        return Arrays.asList(RangerRoles.RANGER_ADMIN, RangerRoles.RANGER_USERSYNC);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getLdapConfig().isPresent() && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

}
