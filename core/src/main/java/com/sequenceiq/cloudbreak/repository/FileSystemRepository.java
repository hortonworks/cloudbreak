package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.FileSystem;

@EntityType(entityClass = FileSystem.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface FileSystemRepository extends CrudRepository<FileSystem, Long> {

    Set<FileSystem> findByOwner(String owner);

    FileSystem findByNameAndOwner(String name, String owner);

    Set<FileSystem> findByAccount(String account);

    Set<FileSystem> findByAccountAndOwner(String account, String owner);

    FileSystem findByNameAndAccountAndOwner(String name, String account, String owner);

}
