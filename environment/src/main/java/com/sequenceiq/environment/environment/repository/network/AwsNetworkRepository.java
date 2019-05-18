package com.sequenceiq.environment.environment.repository.network;

import javax.transaction.Transactional;

import com.sequenceiq.environment.environment.domain.network.AwsNetwork;

@Transactional(Transactional.TxType.REQUIRED)
public interface AwsNetworkRepository extends BaseNetworkRepository<AwsNetwork> {
}
