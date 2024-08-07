package com.sequenceiq.cloudbreak.repository;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = VolumeTemplate.class)
@Transactional(TxType.REQUIRED)
public interface VolumeTemplateRepository extends CrudRepository<VolumeTemplate, Long> {

}
