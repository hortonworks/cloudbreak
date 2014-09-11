package com.sequenceiq.cloudbreak.service.usages;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;

public interface CloudbreakUsagesRetrievalService {
    List<CloudbreakUsage> findUsagesForUser(String user, Long since, String cloud, String zone, String vmtype, String hours);

    List<CloudbreakUsage> findUsagesForAccount(String account, String filterUser, Long since, String cloud, String zone, String vmtype, String hours);

    List<CloudbreakUsage> findUsagesForDeployer(String filterAccount, String filterUser, Long since, String cloud, String zone, String vmtype, String hours);
}
