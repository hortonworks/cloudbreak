package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.service.config.FreeIpaDomainUtils;

@Component
public class DnsLoadBalanceSetupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DnsLoadBalanceSetupService.class);

    public void addDnsLoadBalancedEntries(FreeIpaClient client, FreeIpa freeIpa) throws FreeIpaClientException {
        String domain = freeIpa.getDomain();
        String loadBalancedName = FreeIpaDomainUtils.getBuiltInFreeIpaDnsLoadBalancedName(domain);
        Set<String> cnames = Set.of(
                FreeIpaDomainUtils.getKdcHost(),
                FreeIpaDomainUtils.getKerberosHost(),
                FreeIpaDomainUtils.getLdapHost(),
                FreeIpaDomainUtils.getFreeIpaHost());

        for (String cname : cnames) {
            if (!hasDnsRecord(client, domain, cname)) {
                client.addDnsCnameRecord(domain, cname, loadBalancedName);
                LOGGER.debug("Added DNS cname for {}", cname);
            } else {
                LOGGER.debug("Skipping adding DNS cname for {} because it already exists", cname);
            }
        }
    }

    private boolean hasDnsRecord(FreeIpaClient client, String domain, String cname) throws FreeIpaClientException {
        try {
            client.getDnsRecord(domain, cname);
            return true;
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isNotFoundException(e)) {
                return false;
            }
            throw e;
        }
    }
}
