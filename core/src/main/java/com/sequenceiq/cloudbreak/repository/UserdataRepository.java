package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sequenceiq.cloudbreak.domain.Userdata;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = Userdata.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface UserdataRepository extends JpaRepository<Userdata, Long> {

    @Query("SELECT u FROM Userdata u WHERE u.stack.id = :stackId")
    Optional<Userdata> findByStackId(Long stackId);

}
