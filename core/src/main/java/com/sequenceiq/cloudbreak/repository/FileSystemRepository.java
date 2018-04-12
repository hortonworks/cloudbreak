package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.FileSystem;
import org.springframework.data.repository.CrudRepository;

@EntityType(entityClass = FileSystem.class)
public interface FileSystemRepository extends CrudRepository<FileSystem, Long> {
}
