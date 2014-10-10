package com.sequenceiq.cloudbreak.facade;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.json.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.converter.CloudbreakUsageConverter;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.service.usages.CloudbreakUsageGeneratorService;
import com.sequenceiq.cloudbreak.service.usages.CloudbreakUsagesRetrievalService;

@Service
public class DefaultCloudbreakUsagesFacade implements CloudbreakUsagesFacade {

    @Autowired
    private CloudbreakUsagesRetrievalService cloudbreakUsagesService;

    @Autowired
    private CloudbreakUsageGeneratorService cloudbreakUsageGeneratorService;

    @Autowired
    private CloudbreakUsageConverter cloudbreakUsageConverter;

    @Override
    public List<CloudbreakUsageJson> getUsagesForUser(CbUser user, Long since, String filterUser, String account,
            String cloud, String zone, String vmtype, String hours) {
        List<CloudbreakUsage> usages = cloudbreakUsagesService.findUsagesForUser(user.getUserId(), since, cloud, zone, vmtype, hours);
        return new ArrayList<CloudbreakUsageJson>(cloudbreakUsageConverter.convertAllEntityToJson(usages));
    }

    @Override
    public List<CloudbreakUsageJson> getUsagesForAccount(String account, Long since, String filterUser, String cloud, String zone,
            String vmtype, String hours) {
        List<CloudbreakUsage> usages = cloudbreakUsagesService.findUsagesForAccount(account, filterUser, since, cloud, zone, vmtype, hours);
        return new ArrayList<CloudbreakUsageJson>(cloudbreakUsageConverter.convertAllEntityToJson(usages));
    }

    @Override
    public List<CloudbreakUsageJson> getUsagesForDeployer(CbUser user, Long since, String filterUser, String filterAccount,
            String cloud, String zone, String vmtype, String hours) {
        List<CloudbreakUsage> usages = cloudbreakUsagesService.findUsagesForDeployer(filterAccount, filterUser, since, cloud, zone, vmtype, hours);
        return new ArrayList<CloudbreakUsageJson>(cloudbreakUsageConverter.convertAllEntityToJson(usages));
    }

    @Override
    public void generateUserUsages(CbUser user) {
        cloudbreakUsageGeneratorService.generateCloudbreakUsages(user.getUserId());
    }
}
