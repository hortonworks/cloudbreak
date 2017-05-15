package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.api.model.flex.CloudbreakFlexUsageJson;
import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.service.usages.CloudbreakUsagesRetrievalService;
import com.sequenceiq.cloudbreak.service.usages.FlexUsageGenerator;

@Service
@Transactional
public class DefaultCloudbreakUsagesFacade implements CloudbreakUsagesFacade {

    @Inject
    private CloudbreakUsagesRetrievalService cloudbreakUsagesService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private FlexUsageGenerator flexUsageGenerator;

    @Override
    public List<CloudbreakUsageJson> getUsagesFor(CbUsageFilterParameters params) {
        List<CloudbreakUsage> usages = cloudbreakUsagesService.findUsagesFor(params);
        return (List<CloudbreakUsageJson>) conversionService
                .convert(usages, TypeDescriptor.forObject(usages), TypeDescriptor.collection(List.class,
                        TypeDescriptor.valueOf(CloudbreakUsageJson.class)));
    }

    @Override
    public CloudbreakFlexUsageJson getFlexUsagesFor(CbUsageFilterParameters params) {
        List<CloudbreakUsage> usages = cloudbreakUsagesService.findUsagesFor(params);
        return flexUsageGenerator.getUsages(usages, params.getSince());
    }

}
