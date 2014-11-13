package com.sequenceiq.cloudbreak.facade;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.json.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.converter.CloudbreakUsageConverter;
import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.service.usages.CloudbreakUsageGeneratorService;
import com.sequenceiq.cloudbreak.service.usages.CloudbreakUsagesRetrievalService;

@Service
public class DefaultCloudbreakUsagesFacade implements CloudbreakUsagesFacade {

    @Autowired
    private CloudbreakUsagesRetrievalService cloudbreakUsagesService;

    @Autowired
    private CloudbreakUsageGeneratorService cloudbreakUsageGeneratorService;

    @Autowired
    private CloudbreakUsageConverter cloudbreakUsageConverter;

    @Override
    public List<CloudbreakUsageJson> getUsagesFor(CbUsageFilterParameters params) {

        List<CloudbreakUsage> usages = cloudbreakUsagesService.findUsagesFor(params);
        return new ArrayList<>(cloudbreakUsageConverter.convertAllEntityToJson(usages));
    }

    @Override
    public void generateUserUsages() {
        cloudbreakUsageGeneratorService.generate();
    }
}
