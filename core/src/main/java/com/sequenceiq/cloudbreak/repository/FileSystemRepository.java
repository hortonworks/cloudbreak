package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = FileSystem.class)
@Transactional(Transactional.TxType.REQUIRED)
@HasPermission
public interface FileSystemRepository extends BaseRepository<FileSystem, Long> {

    Set<FileSystem> findByOwner(String owner);

    FileSystem findByNameAndOwner(String name, String owner);

    Set<FileSystem> findByAccount(String account);

    Set<FileSystem> findByAccountAndOwner(String account, String owner);

    FileSystem findByNameAndAccountAndOwner(String name, String account, String owner);

}
