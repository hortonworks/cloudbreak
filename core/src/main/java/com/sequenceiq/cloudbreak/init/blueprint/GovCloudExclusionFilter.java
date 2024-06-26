package com.sequenceiq.cloudbreak.init.blueprint;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.gov.CommonGovService;

@Service
public class GovCloudExclusionFilter {

    @Inject
    private CommonGovService commonGovService;

    @Inject
    private ExclusionListProperties exclusionListProperties;

    public boolean shouldAddBlueprint(String stackVersion, String blueprintName) {
        return commonGovService.govCloudCompatibleVersion(stackVersion) &&
                !exclusionListProperties.isBlueprintExcluded(stackVersion, blueprintName);
    }
}
