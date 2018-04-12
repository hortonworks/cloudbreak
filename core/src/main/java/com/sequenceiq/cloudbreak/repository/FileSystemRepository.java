package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.FileSystem;

@EntityType(entityClass = FileSystem.class)
public interface FileSystemRepository extends CrudRepository<FileSystem, Long> {
}
