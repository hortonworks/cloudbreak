package com.sequenceiq.environment.network.dao.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.environment.network.dao.domain.BaseNetwork;

@Transactional(Transactional.TxType.REQUIRED)
public interface BaseNetworkRepository<T extends BaseNetwork> extends JpaRepository<T, Long> {

    Optional<BaseNetwork> findByEnvironmentId(Long envId);

}
