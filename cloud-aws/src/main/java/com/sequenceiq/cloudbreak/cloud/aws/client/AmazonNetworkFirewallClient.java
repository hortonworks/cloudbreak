package com.sequenceiq.cloudbreak.cloud.aws.client;

import com.amazonaws.services.networkfirewall.AWSNetworkFirewall;
import com.amazonaws.services.networkfirewall.model.DescribeFirewallRequest;
import com.amazonaws.services.networkfirewall.model.DescribeFirewallResult;
import com.amazonaws.services.networkfirewall.model.ListFirewallsRequest;
import com.amazonaws.services.networkfirewall.model.ListFirewallsResult;
import com.sequenceiq.cloudbreak.service.Retry;

public class AmazonNetworkFirewallClient extends AmazonClient {

    private final AWSNetworkFirewall client;

    private final Retry retry;

    public AmazonNetworkFirewallClient(AWSNetworkFirewall client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public ListFirewallsResult listFirewalls(ListFirewallsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.listFirewalls(request));
    }

    public DescribeFirewallResult describeFirewall(DescribeFirewallRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeFirewall(request));
    }
}
