package com.sequenceiq.cloudbreak.service.usages;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;

public interface CloudbreakUsagesRetrievalService {
    List<CloudbreakUsage> findUsagesForUser(Long userId, Long since, String cloud, String zone, String vmtype, String hours);

    List<CloudbreakUsage> findUsagesForAccount(Long accountId, Long filterUserid, Long since, String cloud, String zone, String vmtype, String hours);

    List<CloudbreakUsage> findUsagesForDeployer(Long filterAccountId, Long filterUserid, Long since, String cloud, String zone, String vmtype, String hours);
}
