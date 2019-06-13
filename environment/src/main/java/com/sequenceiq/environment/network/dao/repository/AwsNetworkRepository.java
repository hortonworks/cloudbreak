package com.sequenceiq.environment.network.dao.repository;

import javax.transaction.Transactional;

import com.sequenceiq.environment.network.dao.domain.AwsNetwork;

@Transactional(Transactional.TxType.REQUIRED)
public interface AwsNetworkRepository extends BaseNetworkRepository<AwsNetwork> {
}
