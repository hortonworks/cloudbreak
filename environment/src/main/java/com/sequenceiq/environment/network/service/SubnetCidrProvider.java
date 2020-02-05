package com.sequenceiq.environment.network.service;

import java.util.Set;

/**
 * Provider for calculate subnet CIDRs.
 */
public interface SubnetCidrProvider {

    Set<String> provide(String networkCidr);
}
