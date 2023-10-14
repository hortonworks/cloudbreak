package com.sequenceiq.it.cloudbreak.dto.autoscale;

import java.util.List;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.microservice.PeriscopeClient;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.LoadAlertConfigurationRequest;
import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;

@Prototype
public class AutoScaleConfigDto extends AbstractTestDto<DistroXAutoscaleClusterRequest, DistroXAutoscaleClusterResponse, AutoScaleConfigDto, PeriscopeClient> {

    public AutoScaleConfigDto(TestContext testContext) {
        super(new DistroXAutoscaleClusterRequest(), testContext);
    }

    public AutoScaleConfigDto withEnableAutoScaling(boolean enableAutoScaling) {
        getRequest().setEnableAutoscaling(enableAutoScaling);
        return this;
    }

    public AutoScaleConfigDto withUseStopStartMechanism(boolean useStopStartMechanism) {
        getRequest().setUseStopStartMechanism(useStopStartMechanism);
        return this;
    }

    @Override
    public AutoScaleConfigDto valid() {
        return this;
    }

    public AutoScaleConfigDto withLoadAlert(String alertName, int minNodes, int maxNodes) {
        LoadAlertRequest loadAlertRequest = new LoadAlertRequest();
        loadAlertRequest.setAlertName(alertName);
        LoadAlertConfigurationRequest loadAlertConfigurationRequest = new LoadAlertConfigurationRequest();
        loadAlertConfigurationRequest.setMinResourceValue(minNodes);
        loadAlertConfigurationRequest.setMaxResourceValue(maxNodes);
        loadAlertRequest.setLoadAlertConfiguration(loadAlertConfigurationRequest);
        ScalingPolicyRequest scalingPolicyRequest = new ScalingPolicyRequest();
        scalingPolicyRequest.setAdjustmentType(AdjustmentType.LOAD_BASED);
        scalingPolicyRequest.setHostGroup("compute");
        loadAlertRequest.setScalingPolicy(scalingPolicyRequest);
        getRequest().setLoadAlertRequests(List.of(loadAlertRequest));
        return this;
    }

    @Override
    public AutoScaleConfigDto when(Action<AutoScaleConfigDto, PeriscopeClient> action, RunningParameter runningParameter) {
        return getTestContext().when(this, PeriscopeClient.class, action, runningParameter);
    }

}
