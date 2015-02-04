package com.sequenceiq.cloudbreak.service.usages;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageSpecifications;

@Service
public class DefaultCloudbreakUsagesRetrievalService implements CloudbreakUsagesRetrievalService {

    @Autowired
    private CloudbreakUsageRepository usageRepository;

    @Override
    public List<CloudbreakUsage> findUsagesFor(CbUsageFilterParameters params) {
        List<CloudbreakUsage> usages = usageRepository.findAll(
                Specifications.where(CloudbreakUsageSpecifications.usagesWithStringFields("account", params.getAccount()))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("owner", params.getOwner()))
                .and(CloudbreakUsageSpecifications.usagesSince(params.getSince()))
                .and(CloudbreakUsageSpecifications.usagesBefore(params.getFilterEndDate()))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("provider", params.getCloud()))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("region", params.getRegion())));
        return usages;
    }
}
