package com.sequenceiq.cloudbreak.controller;

import static java.time.ZoneId.systemDefault;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.UsageEndpoint;
import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.api.model.flex.CloudbreakFlexUsageJson;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters;
import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters.Builder;
import com.sequenceiq.cloudbreak.facade.CloudbreakUsagesFacade;

@Component
public class CloudbreakUsageController implements UsageEndpoint {

    @Autowired
    private CloudbreakUsagesFacade cloudbreakUsagesFacade;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public List<CloudbreakUsageJson> getDeployer(
            Long since,
            Long filterEndDate,
            String userId,
            String accountId,
            String cloud,
            String zone) {
        CbUsageFilterParameters params = new Builder().setAccount(accountId).setOwner(userId)
                .setSince(since).setCloud(cloud).setRegion(zone).setFilterEndDate(filterEndDate).build();
        return cloudbreakUsagesFacade.getUsagesFor(params);
    }

    @Override
    public List<CloudbreakUsageJson> getAccount(
            Long since,
            Long filterEndDate,
            String userId,
            String cloud,
            String zone) {
        IdentityUser user = authenticatedUserService.getCbUser();
        CbUsageFilterParameters params = new Builder().setAccount(user.getAccount()).setOwner(userId)
                .setSince(since).setCloud(cloud).setRegion(zone).setFilterEndDate(filterEndDate).build();
        return cloudbreakUsagesFacade.getUsagesFor(params);
    }

    @Override
    public List<CloudbreakUsageJson> getUser(
            Long since,
            Long filterEndDate,
            String cloud,
            String zone) {
        IdentityUser user = authenticatedUserService.getCbUser();
        CbUsageFilterParameters params = new Builder().setAccount(user.getAccount()).setOwner(user.getUserId())
                .setSince(since).setCloud(cloud).setRegion(zone).setFilterEndDate(filterEndDate).build();
        return cloudbreakUsagesFacade.getUsagesFor(params);
    }

    @Override
    public CloudbreakFlexUsageJson getDailyFlexUsages() {
        long fromDate = LocalDate.now()
                .minusDays(1)
                .atStartOfDay(systemDefault())
                .toInstant()
                .toEpochMilli();

        long endDate = LocalDate.now()
                .atStartOfDay(systemDefault())
                .toInstant()
                .toEpochMilli();

        CbUsageFilterParameters cbUsageFilterParameters = new Builder()
                .setSince(fromDate)
                .setFilterEndDate(endDate)
                .build();
        return cloudbreakUsagesFacade.getFlexUsagesFor(cbUsageFilterParameters);
    }

    @Override
    public CloudbreakFlexUsageJson getLatestFlexUsages() {
        long fromDate = LocalDate.now()
                .atStartOfDay(systemDefault())
                .toInstant()
                .toEpochMilli();

        CbUsageFilterParameters cbUsageFilterParameters = new Builder()
                .setSince(fromDate)
                .build();
        return cloudbreakUsagesFacade.getFlexUsagesFor(cbUsageFilterParameters);
    }

}
