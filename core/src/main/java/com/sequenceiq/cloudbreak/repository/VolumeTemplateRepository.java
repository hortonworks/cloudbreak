package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.workspace.repository.BaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.HasPermission;

@EntityType(entityClass = VolumeTemplate.class)
@Transactional(TxType.REQUIRED)
@HasPermission
public interface VolumeTemplateRepository extends BaseRepository<VolumeTemplate, Long> {

}