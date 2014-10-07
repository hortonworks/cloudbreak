package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import com.sequenceiq.cloudbreak.controller.json.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.domain.CbUser;

public interface CloudbreakUsagesFacade {

    List<CloudbreakUsageJson> getUsagesForUser(CbUser user, Long since, String filterUser, String account,
            String cloud, String zone, String vmtype, String hours);

    List<CloudbreakUsageJson> getUsagesForAccount(String account, Long since, String filterUser, String cloud, String zone, String vmtype, String hours);

    List<CloudbreakUsageJson> getUsagesForDeployer(CbUser user, Long since, String filterUser, String account,
            String cloud, String zone, String vmtype, String hours);

    void generateUserUsages(CbUser user);

}
