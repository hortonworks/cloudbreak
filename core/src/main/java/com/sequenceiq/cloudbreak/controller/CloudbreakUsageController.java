package com.sequenceiq.cloudbreak.controller;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.UsageEndpoint;
import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.api.model.flex.CloudbreakFlexUsageJson;
import com.sequenceiq.cloudbreak.facade.CloudbreakUsagesFacade;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;

@Controller
@Transactional(TxType.NEVER)
public class CloudbreakUsageController implements UsageEndpoint {

    @Autowired
    private CloudbreakUsagesFacade cloudbreakUsagesFacade;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public List<CloudbreakUsageJson> getDeployer(
            Long since,
            Long filterEndDate,
            String userId,
            String accountId,
            String cloud,
            String zone) {
        throw new UnsupportedOperationException("This endpoint is not supported yet");
    }

    @Override
    public List<CloudbreakUsageJson> getAccount(
            Long since,
            Long filterEndDate,
            String userId,
            String cloud,
            String zone) {
        throw new UnsupportedOperationException("This endpoint is not supported yet");
    }

    @Override
    public List<CloudbreakUsageJson> getUser(
            Long since,
            Long filterEndDate,
            String cloud,
            String zone) {
        throw new UnsupportedOperationException("This endpoint is not supported yet");
    }

    @Override
    public CloudbreakFlexUsageJson getDailyFlexUsages() {
        throw new UnsupportedOperationException("This endpoint is not supported yet");
    }

    @Override
    public CloudbreakFlexUsageJson getLatestFlexUsages() {
        throw new UnsupportedOperationException("This endpoint is not supported yet");
    }

}
