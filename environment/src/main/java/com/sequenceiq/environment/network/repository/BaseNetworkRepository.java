package com.sequenceiq.environment.network.repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.environment.network.domain.BaseNetwork;

@Transactional(Transactional.TxType.REQUIRED)
public interface BaseNetworkRepository<T extends BaseNetwork> extends JpaRepository<T, Long> {
}
