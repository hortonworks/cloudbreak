package com.sequenceiq.remoteenvironment.repository;


import jakarta.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;

@Transactional(Transactional.TxType.REQUIRED)
@EntityType(entityClass = PrivateControlPlane.class)
public interface PrivateControlPlaneRepository extends CrudRepository<PrivateControlPlane, Long> {

    void deleteByResourceCrn(String crn);
}
