package com.sequenceiq.cloudbreak.service.usages;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageSpecifications;

@Service
public class DefaultCloudbreakUsagesRetrievalService implements CloudbreakUsagesRetrievalService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakUsagesRetrievalService.class);

    @Autowired
    private CloudbreakUsageRepository usageRepository;

    @Override
    public List<CloudbreakUsage> findUsagesForUser(String user, Long since, String cloud, String zone, String vmtype, String hours) {

        List<CloudbreakUsage> usages = usageRepository.findAll(Specifications
                .where(CloudbreakUsageSpecifications.usagesWithStringFields("userId", user))
                .and(CloudbreakUsageSpecifications.usagesSince(since))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("cloud", cloud))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("zone", zone))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("machineType", vmtype))
                .and(CloudbreakUsageSpecifications.usagesWithStringFields("runningHours", hours)));

        return usages;
    }

    @Override
    public List<CloudbreakUsage> findUsagesForAccount(String account, String filterUser, Long since, String cloud, String zone, String vmtype, String hours) {
        List<CloudbreakUsage> usages = new ArrayList<>();
//        Set<User> accountUsers = accountRepository.accountUsers(account);
//        for (User user : accountUsers) {
//            usages.addAll(findUsagesForUser(user.getId(), since, cloud, zone, vmtype, hours));
//        }
        return usages;
    }

    @Override
    public List<CloudbreakUsage> findUsagesForDeployer(String filterAccount, String filterUser, Long since, String cloud, String zone,
            String vmtype, String hours) {
        List<CloudbreakUsage> usages = new ArrayList<>();
//        Iterator<Account> accountIt = accountRepository.findAll().iterator();
//
//        while (accountIt.hasNext()) {
//            usages.addAll(findUsagesForAccount(accountIt.next().getId(), filterUser, since, cloud, zone, vmtype, hours));
//        }

        return usages;
    }
}
