package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import com.sequenceiq.cloudbreak.controller.json.CloudbreakUsageJson;

public interface CloudbreakUsagesFacade {

    List<CloudbreakUsageJson> getUsagesFor(String account, String owner,
            Long since, String cloud, String zone, String vmtype, String hours, String bpName, Long bpId);

    void generateUserUsages();

}
