package com.sequenceiq.cloudbreak.cloud.aws.view;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

public class AwsCloudStackView {

    private final CloudStack cloudStack;

    public AwsCloudStackView(CloudStack cloudStack) {
        this.cloudStack = cloudStack;
    }

    public AwsNetworkView network() {
        return new AwsNetworkView(cloudStack.getNetwork());
    }

    public OutboundInternetTraffic outboundInternetTraffic() {
        return network().getOutboundInternetTraffic();
    }

    public List<String> cloudSecurityIds() {
        return cloudStack.getCloudSecurity().getCloudSecurityIds();
    }

    public Map<String, String> getTags() {
        return cloudStack.getTags();
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }
}
