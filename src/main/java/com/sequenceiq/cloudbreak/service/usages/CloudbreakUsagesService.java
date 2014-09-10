package com.sequenceiq.cloudbreak.service.usages;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;

public interface CloudbreakUsagesService {
    List<CloudbreakUsage> findUsages(Long userId, Long since, Long accountId, String cloud, String zone, String vmtype, String hours);

    List<CloudbreakUsage> findAccountUsages(Long userId, Long since, Long accountId, String cloud, String zone, String vmtype, String hours);

    List<CloudbreakUsage> findUserUsages(Long userId, Long since, Long accountId, String cloud, String zone, String vmtype, String hours);
}
