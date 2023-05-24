package com.sequenceiq.redbeams.repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.redbeams.domain.stack.SslConfig;

@Transactional(Transactional.TxType.REQUIRED)
public interface SslConfigRepository extends JpaRepository<SslConfig, Long> {
}
