package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import com.sequenceiq.cloudbreak.controller.json.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.domain.User;

public interface CloudbreakUsagesFacade {

    List<CloudbreakUsageJson> loadUsages(User user, Long since, Long filterUserId, Long accountId,
            String cloud, String zone, String vmtype, String hours);

    List<CloudbreakUsageJson> loadAccountUsages(User user, Long since, Long filterUserId, Long accountId,
            String cloud, String zone, String vmtype, String hours);

    List<CloudbreakUsageJson> loadUserUsages(User user, Long since, Long filterUserId, Long accountId,
            String cloud, String zone, String vmtype, String hours);

    void generateUserUsages(User user);

}
