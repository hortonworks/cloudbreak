package com.sequenceiq.redbeams.repository;

import com.sequenceiq.redbeams.domain.stack.DBStack;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

@Transactional(Transactional.TxType.REQUIRED)
public interface DBStackRepository extends JpaRepository<DBStack, Long> {

    Optional<DBStack> findByNameAndEnvironmentId(String name, String environmentId);
}
