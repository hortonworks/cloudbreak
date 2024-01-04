package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.cloudbreak.domain.Userdata;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = Userdata.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface UserdataRepository extends JpaRepository<Userdata, Long> {

    Optional<Userdata> findByStackId(Long stackId);

}
