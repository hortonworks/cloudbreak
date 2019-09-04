package com.sequenceiq.environment.network.dao.repository;

import javax.transaction.Transactional;

import com.sequenceiq.environment.network.dao.domain.AzureNetwork;

@Transactional(Transactional.TxType.REQUIRED)
public interface AzureNetworkRepository extends BaseNetworkRepository<AzureNetwork> {
}
