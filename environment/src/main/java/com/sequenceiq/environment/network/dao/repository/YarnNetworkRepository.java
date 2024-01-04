package com.sequenceiq.environment.network.dao.repository;

import jakarta.transaction.Transactional;

import com.sequenceiq.environment.network.dao.domain.YarnNetwork;

@Transactional(Transactional.TxType.REQUIRED)
public interface YarnNetworkRepository extends BaseNetworkRepository<YarnNetwork> {
}
