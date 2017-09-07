package com.sequenceiq.cloudbreak.service.cluster.flow.kerberos;

import org.springframework.stereotype.Service;

@Service
public class KerberosDomainResolver {

    public String getDomains(String gwDomain) {
        return '.' + gwDomain;
    }
}
