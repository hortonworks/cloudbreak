package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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

    @Inject
    private FreeIpaConfigProvider freeIpaConfigProvider;

    @Inject
    private KerberosDetailService kerberosDetailService;

    public Map<String, SaltPillarProperties> createSssdIpaPillar(KerberosConfig kerberosConfig, Map<String, List<String>> serviceLocations,
            String environmentCrn) {
        if (kerberosDetailService.isIpaJoinable(kerberosConfig)) {
            Map<String, Object> sssdConfig = new HashMap<>();
            sssdConfig.put("principal", kerberosConfig.getPrincipal());
            sssdConfig.put("realm", kerberosConfig.getRealm().toUpperCase());
            sssdConfig.put("domain", kerberosConfig.getDomain());
            sssdConfig.put("password", kerberosConfig.getPassword());
            sssdConfig.put("server", kerberosConfig.getUrl());
            sssdConfig.put("dns_ttl", kerberosDetailService.getDnsTtl());
            // enumeration has performance impacts so it's only enabled if Ranger is installed on the cluster
            // otherwise the usersync does not work with nss
            boolean enumerate = !CollectionUtils.isEmpty(serviceLocations.get("RANGER_ADMIN"))
                    || !CollectionUtils.isEmpty(serviceLocations.get("NIFI_REGISTRY_SERVER"))
                    || !CollectionUtils.isEmpty(serviceLocations.get("NIFI_NODE"));
            sssdConfig.put("enumerate", enumerate);
            sssdConfig.put("entryCacheTimeout", entryCacheTimeout);
            sssdConfig.put("memcacheTimeout", memcacheTimeout);
            sssdConfig.put("heartbeatTimeout", heartbeatTimeout);
            Map<String, Object> freeIpaConfig = freeIpaConfigProvider.createFreeIpaConfig(environmentCrn);
            return Map.of("sssd-ipa", new SaltPillarProperties("/sssd/ipa.sls",
                    Map.of("sssd-ipa", sssdConfig, "freeipa", freeIpaConfig)));
        } else {
            return Map.of();
        }
    }

    public Map<String, SaltPillarProperties> createSssdAdPillar(KerberosConfig kerberosConfig) {
        if (kerberosDetailService.isAdJoinable(kerberosConfig)) {
            Map<String, Object> sssdConnfig = new HashMap<>();
            sssdConnfig.put("username", kerberosConfig.getPrincipal());
            sssdConnfig.put("domainuppercase", kerberosConfig.getRealm().toUpperCase());
            sssdConnfig.put("domain", kerberosConfig.getRealm().toLowerCase());
            sssdConnfig.put("password", kerberosConfig.getPassword());
            return Map.of("sssd-ad", new SaltPillarProperties("/sssd/ad.sls", singletonMap("sssd-ad", sssdConnfig)));
        } else {
            return Map.of();
        }
    }
}
