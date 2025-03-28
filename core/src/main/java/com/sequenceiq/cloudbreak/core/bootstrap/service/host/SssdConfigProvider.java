package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_18;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerUserSyncRoleConfigProvider.LDAPUSERSYNC_PROCESS_LDAP_USER_GROUP_BUILDER;
import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaConfigProvider;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;

@Component
public class SssdConfigProvider {

    @Value("${sssd.entry.cache.timeout}")
    private int entryCacheTimeout;

    @Value("${sssd.memcache.timeout}")
    private int memcacheTimeout;

    @Value("${sssd.heartbeat.timeout}")
    private int heartbeatTimeout;

    @Value("${sssd.nss.timeout}")
    private int nssTimeout;

    @Inject
    private FreeIpaConfigProvider freeIpaConfigProvider;

    @Inject
    private KerberosDetailService kerberosDetailService;

    @Inject
    private EntitlementService entitlementService;

    public Map<String, SaltPillarProperties> createSssdIpaPillar(KerberosConfig kerberosConfig, Map<String, List<String>> serviceLocations,
            String environmentCrn, String stackVersion, String extendedBlueprintText) {
        if (kerberosDetailService.isIpaJoinable(kerberosConfig)) {
            Map<String, Object> sssdConfig = new HashMap<>();
            sssdConfig.put("principal", kerberosConfig.getPrincipal());
            sssdConfig.put("realm", kerberosConfig.getRealm().toUpperCase(Locale.ROOT));
            sssdConfig.put("domain", kerberosConfig.getDomain());
            sssdConfig.put("password", kerberosConfig.getPassword());
            sssdConfig.put("server", kerberosConfig.getUrl());
            sssdConfig.put("dns_ttl", kerberosDetailService.getDnsTtl());
            // enumeration has performance impacts so it's only enabled if Ranger is installed on the cluster
            // otherwise the usersync does not work with nss
            boolean enumerate = isEnumerate(serviceLocations, stackVersion, extendedBlueprintText, environmentCrn);
            sssdConfig.put("enumerate", enumerate);
            sssdConfig.put("entryCacheTimeout", entryCacheTimeout);
            sssdConfig.put("memcacheTimeout", memcacheTimeout);
            sssdConfig.put("heartbeatTimeout", heartbeatTimeout);
            sssdConfig.put("nssTimeout", nssTimeout);
            Map<String, Object> freeIpaConfig = freeIpaConfigProvider.createFreeIpaConfig(environmentCrn);
            return Map.of("sssd-ipa", new SaltPillarProperties("/sssd/ipa.sls",
                    Map.of("sssd-ipa", sssdConfig, "freeipa", freeIpaConfig)));
        } else {
            return Map.of();
        }
    }

    private boolean isEnumerate(Map<String, List<String>> serviceLocations, String stackVersion, String extendedBlueprintText, String environmentCrn) {
        if (StringUtils.isNotBlank(stackVersion) && isVersionNewerOrEqualThanLimited(stackVersion, CLOUDERA_STACK_VERSION_7_2_18)) {
            boolean rangerLdapUserSyncEnabled = (Objects.nonNull(extendedBlueprintText) ?
                    extendedBlueprintText.contains(LDAPUSERSYNC_PROCESS_LDAP_USER_GROUP_BUILDER) : true)
                    && entitlementService.isRangerLdapUsersyncEnabled(Crn.safeFromString(environmentCrn).getAccountId());
            return isRangerAdminPresented(serviceLocations) && !rangerLdapUserSyncEnabled;
        } else {
            return isRangerAdminPresented(serviceLocations)
                    || !CollectionUtils.isEmpty(serviceLocations.get("NIFI_REGISTRY_SERVER"))
                    || !CollectionUtils.isEmpty(serviceLocations.get("NIFI_NODE"));
        }
    }

    private boolean isRangerAdminPresented(Map<String, List<String>> serviceLocations) {
        return !CollectionUtils.isEmpty(serviceLocations.get("RANGER_ADMIN"));
    }

    public Map<String, SaltPillarProperties> createSssdAdPillar(KerberosConfig kerberosConfig) {
        if (kerberosDetailService.isAdJoinable(kerberosConfig)) {
            Map<String, Object> sssdConnfig = new HashMap<>();
            sssdConnfig.put("username", kerberosConfig.getPrincipal());
            sssdConnfig.put("domainuppercase", kerberosConfig.getRealm().toUpperCase(Locale.ROOT));
            sssdConnfig.put("domain", kerberosConfig.getRealm().toLowerCase(Locale.ROOT));
            sssdConnfig.put("password", kerberosConfig.getPassword());
            return Map.of("sssd-ad", new SaltPillarProperties("/sssd/ad.sls", singletonMap("sssd-ad", sssdConnfig)));
        } else {
            return Map.of();
        }
    }
}
