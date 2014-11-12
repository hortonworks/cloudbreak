package com.sequenceiq.cloudbreak.service.usages;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;

public interface CloudbreakUsagesRetrievalService {
    List<CloudbreakUsage> findUsagesFor(String account, String owner,
            Long since, String cloud, String zone, String vmtype, String hours, String bpName, Long bpId);
}
