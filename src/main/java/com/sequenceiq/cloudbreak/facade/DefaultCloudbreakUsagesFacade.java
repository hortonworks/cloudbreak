package com.sequenceiq.cloudbreak.facade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.json.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.converter.CloudbreakUsageConverter;
import com.sequenceiq.cloudbreak.domain.Account;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.service.account.AccountService;
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

    @Autowired
    private AccountService accountService;


    @Override
    public List<CloudbreakUsageJson> getUsagesForUser(User user, Long since, Long filterUserId, Long accountId,
            String cloud, String zone, String vmtype, String hours) {
        List<CloudbreakUsage> usages = cloudbreakUsagesService.findUsagesForUser(user.getId(), since, cloud, zone, vmtype, hours);
        return new ArrayList<CloudbreakUsageJson>(cloudbreakUsageConverter.convertAllEntityToJson(usages));
    }

    @Override
    public List<CloudbreakUsageJson> getUsagesForAccount(Long accountId, Long since, Long filterUserId, String cloud, String zone,
            String vmtype, String hours) {
        List<CloudbreakUsage> usages = new ArrayList<>();
        Set<User> accountUsers = accountService.accountUsers(accountId);
        for (User accountUser : accountUsers) {
            usages.addAll(cloudbreakUsagesService.findUsagesForUser(accountUser.getId(), since, cloud, zone, vmtype, hours));
        }
        return new ArrayList<CloudbreakUsageJson>(cloudbreakUsageConverter.convertAllEntityToJson(usages));
    }

    @Override
    public List<CloudbreakUsageJson> getUsagesForDeployer(User user, Long since, Long filterUserId, Long filterAccountId,
            String cloud, String zone, String vmtype, String hours) {
        List<CloudbreakUsage> usages = new ArrayList<>();

        List<Account> accounts = accountService.getAccounts();
        for (Account account : accounts) {

        }


        return new ArrayList<CloudbreakUsageJson>(cloudbreakUsageConverter.convertAllEntityToJson(usages));
    }

    @Override
    public void generateUserUsages(User user) {
        cloudbreakUsageGeneratorService.generateCloudbreakUsages(user.getId());
    }
}
