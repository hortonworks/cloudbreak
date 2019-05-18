package com.sequenceiq.environment.environment.repository.network;

import javax.transaction.Transactional;

import com.sequenceiq.environment.environment.domain.network.AzureNetwork;

@Transactional(Transactional.TxType.REQUIRED)
public interface AzureNetworkRepository extends BaseNetworkRepository<AzureNetwork> {
}
