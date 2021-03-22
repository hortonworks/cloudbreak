package com.sequenceiq.cloudbreak.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

@Service
public class ServiceEndpointCollectorEntitlementComparator {

    public boolean entitlementSupported(List<String> entitlements, String entitlement) {
        boolean shouldInclude = false;
        if (Strings.isNullOrEmpty(entitlement)) {
            shouldInclude = true;
        } else if (entitlements == null || entitlements.isEmpty()) {
            shouldInclude =  true;
        } else if (entitlements.contains(entitlement)) {
            shouldInclude =  true;
        }
        return shouldInclude;
    }
}
