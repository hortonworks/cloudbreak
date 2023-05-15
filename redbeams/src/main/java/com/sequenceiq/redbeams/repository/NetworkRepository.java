package com.sequenceiq.redbeams.repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.redbeams.domain.stack.Network;

@Transactional(Transactional.TxType.REQUIRED)
public interface NetworkRepository extends JpaRepository<Network, Long> {
}
