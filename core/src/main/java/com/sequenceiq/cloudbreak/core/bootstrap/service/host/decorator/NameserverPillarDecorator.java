package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.util.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustResponse;

@Component
public class NameserverPillarDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NameserverPillarDecorator.class);

    @Inject
    private FreeipaClientService freeipaClient;

    public Map<String, SaltPillarProperties> createPillarForNameservers(KerberosConfig kerberosConfig, String environmentCrn) {
        if (kerberosConfig != null && StringUtils.isNotBlank(kerberosConfig.getDomain()) && StringUtils.isNotBlank(kerberosConfig.getNameServers())) {
            LOGGER.debug("Add nameserver config to pillar based on kerberos config.");
            List<String> ipList = getKerberosNameServerIps(kerberosConfig);
            validateIpList(ipList);
            Map<String, Map<String, List<String>>> forwarderZones = new HashMap<>();
            Map<String, List<String>> defaultReverseZone = new HashMap<>();
            forwarderZones.put(kerberosConfig.getDomain(), singletonMap("nameservers", ipList));
            getTrustedRealm(environmentCrn).ifPresent(trustedRealm -> {
                LOGGER.debug("Adding nameservers for trusted realm {}", trustedRealm);
                forwarderZones.put(trustedRealm.toLowerCase(Locale.ROOT), singletonMap("nameservers", ipList));
                defaultReverseZone.put("nameservers", ipList);
            });
            return Map.of("forwarder-zones",
                    new SaltPillarProperties("/unbound/forwarders.sls", Map.of(
                            "forwarder-zones", forwarderZones,
                            "default-reverse-zone", defaultReverseZone)));
        } else {
            LOGGER.debug("Skip to add nameserver config to pillar for kerberos config {}", kerberosConfig);
            return Map.of();
        }
    }

    private Optional<String> getTrustedRealm(String environmentCrn) {
        return freeipaClient.findByEnvironmentCrn(environmentCrn)
                .map(DescribeFreeIpaResponse::getTrust)
                .map(TrustResponse::getRealm)
                .filter(StringUtils::isNotBlank);
    }

    private List<String> getKerberosNameServerIps(KerberosConfig kerberosConfig) {
        return Lists.newArrayList(kerberosConfig.getNameServers().split(","))
                .stream()
                .filter(filterValidIps())
                .collect(Collectors.toList());
    }

    private Predicate<String> filterValidIps() {
        return ip -> StringUtils.isNotBlank(ip) && InetAddressUtils.isIPv4Address(ip);
    }

    private void validateIpList(List<String> ipList) {
        if (ipList.isEmpty()) {
            String message = "Unable to setup nameservers because there is no IP address present.";
            LOGGER.error(message);
            throw new CloudbreakServiceException(message);
        }
    }

}
