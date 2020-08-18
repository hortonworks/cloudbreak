package com.sequenceiq.environment.network.dao.repository;

import javax.transaction.Transactional;

import com.sequenceiq.environment.network.dao.domain.GcpNetwork;

@Transactional(Transactional.TxType.REQUIRED)
public interface GcpNetworkRepository extends BaseNetworkRepository<GcpNetwork> {
}
