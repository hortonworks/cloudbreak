package com.sequenceiq.cloudbreak.service.usages;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageSpecifications;

@Service
public class DefaultCloudbreakUsagesRetrievalService implements CloudbreakUsagesRetrievalService {

    @Autowired
    private CloudbreakUsageRepository usageRepository;

    @Override
    public List<CloudbreakUsage> findUsagesFor(String account, String owner, Long since, String cloud,
            String zone, String vmtype, String hours, String bpName, Long bpId) {

        List<CloudbreakUsage> usages = usageRepository.findAll(
                Specifications.where(CloudbreakUsageSpecifications.usagesWithStringFields("account", account))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("owner", owner))
                .and(CloudbreakUsageSpecifications.usagesSince(since))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("cloud", cloud))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("zone", zone))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("machineType", vmtype))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("runningHours", hours))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("blueprintName", bpName))
                .and(CloudbreakUsageSpecifications.usagesWithLongField("blueprintId", bpId)));
        return usages;
    }
}
