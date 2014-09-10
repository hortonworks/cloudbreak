package com.sequenceiq.cloudbreak.facade;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.json.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.converter.CloudbreakUsageConverter;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.service.usages.CloudbreakUsagesService;

@Service
public class DefaultCloudbreakUsagesFacade implements CloudbreakUsagesFacade {

    @Autowired
    private CloudbreakUsagesService cloudbreakUsagesService;

    @Autowired
    private CloudbreakUsageConverter cloudbreakUsageConverter;


    @Override
    public List<CloudbreakUsageJson> loadUsages(User user, Long since, Long filterUserId, Long accountId,
            String cloud, String zone, String vmtype, String hours) {
        List<CloudbreakUsage> usages = cloudbreakUsagesService.findUsages(user.getId(), since, accountId, cloud, zone, vmtype, hours);
        return new ArrayList<CloudbreakUsageJson>(cloudbreakUsageConverter.convertAllEntityToJson(usages));
    }

    @Override
    public List<CloudbreakUsageJson> loadAccountUsages(User user, Long since, Long filterUserId, Long accountId,
            String cloud, String zone, String vmtype, String hours) {
        List<CloudbreakUsage> usages = cloudbreakUsagesService.findAccountUsages(user.getId(), since, accountId, cloud, zone, vmtype, hours);
        return new ArrayList<CloudbreakUsageJson>(cloudbreakUsageConverter.convertAllEntityToJson(usages));
    }

    @Override
    public List<CloudbreakUsageJson> loadUserUsages(User user, Long since, Long filterUserId, Long accountId,
            String cloud, String zone, String vmtype, String hours) {
        List<CloudbreakUsage> usages = cloudbreakUsagesService.findUserUsages(user.getId(), since, accountId, cloud, zone, vmtype, hours);
        return new ArrayList<CloudbreakUsageJson>(cloudbreakUsageConverter.convertAllEntityToJson(usages));
    }
}
