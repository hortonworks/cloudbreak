package com.sequenceiq.redbeams.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@Transactional(Transactional.TxType.REQUIRED)
public interface DBStackRepository extends JpaRepository<DBStack, Long> {

    Optional<DBStack> findByNameAndEnvironmentId(String name, String environmentId);

    Optional<DBStack> findByResourceCrn(Crn crn);

}
