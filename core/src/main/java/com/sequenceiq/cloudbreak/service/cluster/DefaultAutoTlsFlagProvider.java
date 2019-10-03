package com.sequenceiq.cloudbreak.service.cluster;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Service
public class DefaultAutoTlsFlagProvider {

    public boolean defaultAutoTls(String cloudPlatform) {
        return !CloudPlatform.YARN.name().equals(cloudPlatform);
    }
}
