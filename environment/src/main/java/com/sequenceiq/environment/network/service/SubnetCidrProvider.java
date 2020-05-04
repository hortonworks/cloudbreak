package com.sequenceiq.environment.network.service;

/**
 * Provider for calculate subnet CIDRs.
 */
public interface SubnetCidrProvider {

    Cidrs provide(String networkCidr, boolean privateSubnetEnabled);

    String cloudPlatform();
}
