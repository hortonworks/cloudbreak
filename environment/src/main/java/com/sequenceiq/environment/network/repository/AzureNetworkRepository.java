package com.sequenceiq.environment.network.repository;

import javax.transaction.Transactional;

import com.sequenceiq.environment.network.AzureNetwork;

@Transactional(Transactional.TxType.REQUIRED)
public interface AzureNetworkRepository extends BaseNetworkRepository<AzureNetwork> {
}
