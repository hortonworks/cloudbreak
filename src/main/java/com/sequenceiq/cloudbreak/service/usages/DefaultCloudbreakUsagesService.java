package com.sequenceiq.cloudbreak.service.usages;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageSpecifications;

@Service
public class DefaultCloudbreakUsagesService implements CloudbreakUsagesService {

    @Autowired
    private CloudbreakUsageRepository cloudbreakUsageRepository;

    @Override
    public List<CloudbreakUsage> findUsages(Long userId, Long since, Long accountId, String cloud, String zone, String vmtype, String hours) {

        List<CloudbreakUsage> usages = cloudbreakUsageRepository.findAll(Specifications
                .where(CloudbreakUsageSpecifications.usagesWithLongField("userId", userId))
                .and(CloudbreakUsageSpecifications.usagesWithLongField("accountId", accountId))
                .and(CloudbreakUsageSpecifications.usagesSince(since))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("cloud", cloud))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("zone", zone))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("machineType", vmtype))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("runningHours", hours)));

        return usages;
    }

    @Override
    public List<CloudbreakUsage> findAccountUsages(Long userId, Long since, Long accountId, String cloud, String zone, String vmtype, String hours) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public List<CloudbreakUsage> findUserUsages(Long userId, Long since, Long accountId, String cloud, String zone, String vmtype, String hours) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }
}
