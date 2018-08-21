package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = FileSystem.class)
@Transactional(Transactional.TxType.REQUIRED)
@HasPermission
public interface FileSystemRepository extends OrganizationResourceRepository<FileSystem, Long> {

}
