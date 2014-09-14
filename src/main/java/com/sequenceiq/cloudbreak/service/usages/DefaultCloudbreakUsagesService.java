package com.sequenceiq.cloudbreak.service.usages;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageSpecifications;

@Service
public class DefaultCloudbreakUsagesService implements CloudbreakUsagesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakUsagesService.class);

    @Autowired
    private CloudbreakUsageRepository usageRepository;

    @Autowired
    private CloudbreakEventRepository eventRepository;

    @Override
    public List<CloudbreakUsage> findUsages(Long userId, Long since, Long accountId, String cloud, String zone, String vmtype, String hours) {

        List<CloudbreakUsage> usages = usageRepository.findAll(Specifications
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
