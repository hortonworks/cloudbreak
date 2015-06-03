package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.json.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.service.usages.CloudbreakUsageGeneratorService;
import com.sequenceiq.cloudbreak.service.usages.CloudbreakUsagesRetrievalService;

@Service
public class DefaultCloudbreakUsagesFacade implements CloudbreakUsagesFacade {

    @Inject
    private CloudbreakUsagesRetrievalService cloudbreakUsagesService;

    @Inject
    private CloudbreakUsageGeneratorService cloudbreakUsageGeneratorService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public List<CloudbreakUsageJson> getUsagesFor(CbUsageFilterParameters params) {
        List<CloudbreakUsage> usages = cloudbreakUsagesService.findUsagesFor(params);
        return (List<CloudbreakUsageJson>) conversionService
                .convert(usages, TypeDescriptor.forObject(usages), TypeDescriptor.collection(List.class,
                        TypeDescriptor.valueOf(CloudbreakUsageJson.class)));
    }

    @Override
    public void generateUserUsages() {
        cloudbreakUsageGeneratorService.generate();
    }
}
