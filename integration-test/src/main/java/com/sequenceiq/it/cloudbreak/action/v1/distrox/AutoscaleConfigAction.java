package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.autoscale.AutoScaleConfigDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.PeriscopeClient;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterResponse;

public class AutoscaleConfigAction implements Action<AutoScaleConfigDto, PeriscopeClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoscaleConfigAction.class);

    @Override
    public AutoScaleConfigDto action(TestContext testContext, AutoScaleConfigDto testDto, PeriscopeClient client) throws Exception {
        Log.whenJson(LOGGER, " DistroXAutoscaleClusterRequest : ", testDto.getRequest());
        DistroXAutoscaleClusterResponse distroXAutoscaleClusterResponse = client.getDefaultClient(testContext)
                .withCrn(testContext.getActingUserCrn().toString()).distroXAutoScaleClusterV1Endpoint()
                .updateAutoscaleConfigByClusterName(testDto.getName(), testDto.getRequest());
        Log.whenJson(LOGGER, " DistroXAutoscaleClusterResponse: ", distroXAutoscaleClusterResponse);
        return testDto;
    }
}
