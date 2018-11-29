package com.sequenceiq.cloudbreak.controller;

import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.UsageEndpoint;
import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.api.model.flex.CloudbreakFlexUsageJson;

@Controller
@Transactional(TxType.NEVER)
public class CloudbreakUsageController implements UsageEndpoint {

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
