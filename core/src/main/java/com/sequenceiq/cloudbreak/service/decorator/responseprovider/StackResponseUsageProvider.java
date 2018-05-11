package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponseEntries;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository;

@Component
public class StackResponseUsageProvider implements ResponseProvider {

    @Inject
    private CloudbreakUsageRepository cloudbreakUsageRepository;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public StackResponse providerEntriesToStackResponse(Stack stack, StackResponse stackResponse) {
        List<CloudbreakUsage> openCloudbreakUsages = cloudbreakUsageRepository.findOpensForStack(stack.getId());
        List<CloudbreakUsage> closedCloudbreakUsages = cloudbreakUsageRepository.findStoppedForStack(stack.getId());

        List<CloudbreakUsageJson> cloudbreakUsagesJsons = new ArrayList<>();
        for (CloudbreakUsage openCloudbreakUsage : openCloudbreakUsages) {
            cloudbreakUsagesJsons.add(conversionService.convert(openCloudbreakUsage, CloudbreakUsageJson.class));
        }
        for (CloudbreakUsage closedCloudbreakUsage : closedCloudbreakUsages) {
            cloudbreakUsagesJsons.add(conversionService.convert(closedCloudbreakUsage, CloudbreakUsageJson.class));
        }
        stackResponse.setCloudbreakUsages(cloudbreakUsagesJsons);
        return stackResponse;
    }

    @Override
    public String type() {
        return StackResponseEntries.USAGES.getEntryName();
    }
}
