package com.sequenceiq.cloudbreak.orchestrator.salt.utils;

import java.util.Comparator;

import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;

public class GatewayConfigComparator implements Comparator<GatewayConfig> {

    @Override
    public int compare(GatewayConfig gw1, GatewayConfig gw2) {
        String saltVersion1 = gw1.getSaltVersion().orElse("");
        String saltVersion2 = gw2.getSaltVersion().orElse("");

        int saltComparison = saltVersion2.compareTo(saltVersion1);
        if (saltComparison == 0) {
            // If same salt version, prioritize primary
            boolean primary1 = gw1.isPrimary();
            boolean primary2 = gw2.isPrimary();
            return Boolean.compare(primary2, primary1);
        }
        return saltComparison;

    }
}
