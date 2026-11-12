package com.sequenceiq.cloudbreak.orchestrator.salt.utils;

import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.util.VersionComparator;

public class GatewayConfigComparator implements Comparator<GatewayConfig> {

    private static final Comparator<GatewayConfig> NEWEST_SALT_VERSION_THEN_PRIMARY_FIRST = Comparator
            .comparing(GatewayConfigComparator::getSaltVersion, Comparator.nullsLast(new VersionComparator().reversed()))
            .thenComparing(GatewayConfig::isPrimary, Comparator.reverseOrder());

    @Override
    public int compare(GatewayConfig gw1, GatewayConfig gw2) {
        return NEWEST_SALT_VERSION_THEN_PRIMARY_FIRST.compare(gw1, gw2);
    }

    private static Versioned getSaltVersion(GatewayConfig gatewayConfig) {
        return gatewayConfig.getSaltVersion()
                .filter(StringUtils::isNotBlank)
                .<Versioned>map(saltVersion -> () -> saltVersion)
                .orElse(null);
    }
}
