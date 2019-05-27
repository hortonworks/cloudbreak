package com.sequenceiq.environment.network.repository;

import javax.transaction.Transactional;

import com.sequenceiq.environment.network.domain.AwsNetwork;

@Transactional(Transactional.TxType.REQUIRED)
public interface AwsNetworkRepository extends BaseNetworkRepository<AwsNetwork> {
}
