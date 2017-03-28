package com.sequenceiq.cloudbreak.facade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.CloudbreakFlexUsageJson;
import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.service.usages.CloudbreakUsagesRetrievalService;

@Service
@Transactional
public class DefaultCloudbreakUsagesFacade implements CloudbreakUsagesFacade {

    @Inject
    private CloudbreakUsagesRetrievalService cloudbreakUsagesService;

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
    public List<CloudbreakFlexUsageJson> getFlexUsagesFor(CbUsageFilterParameters params) {
        List<CloudbreakUsage> usages = cloudbreakUsagesService.findUsagesFor(params);
        Map<Long, CloudbreakFlexUsageJson> flexUsageJsonsByStackId = new HashMap<>();
        for (CloudbreakUsage usage : usages) {
            Long stackId = usage.getStackId();
            if (!flexUsageJsonsByStackId.containsKey(stackId)) {
                CloudbreakFlexUsageJson flexUsageJson = conversionService.convert(usage, CloudbreakFlexUsageJson.class);
                flexUsageJsonsByStackId.put(stackId, flexUsageJson);
            } else {
                CloudbreakFlexUsageJson flexUsageJson = flexUsageJsonsByStackId.get(stackId);
                int peak = flexUsageJson.getPeak() + usage.getPeak();
                int instanceNum = flexUsageJson.getInstanceNum() + usage.getInstanceNum();
                flexUsageJson.setPeak(peak);
                flexUsageJson.setInstanceNum(instanceNum);
            }
        }
        return flexUsageJsonsByStackId.values().stream().collect(Collectors.toList());
    }

}
