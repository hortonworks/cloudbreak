package com.sequenceiq.cloudbreak.service.blueprint;

import java.util.Locale;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;

@Component
public class CrnGeneratorService {

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    public String createBlueprintCrn(String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.CLUSTER_TEMPLATE, accountId);
    }

    public String createGlobalDefaultBlueprintCrn(String name) {
        String resourceId = name.toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9]", "");
        return regionAwareCrnGenerator.generateCrnString(CrnResourceDescriptor.CLUSTER_TEMPLATE, resourceId, "cloudera_default");
    }

    public String createGlobalDefaultClusterDefinitionCrn(String name) {
        String resourceId = name.toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9]", "");
        return regionAwareCrnGenerator.generateCrnString(CrnResourceDescriptor.CLUSTER_DEF, resourceId, "cloudera_default");
    }
}
