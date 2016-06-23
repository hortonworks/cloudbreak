package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository

import com.sequenceiq.cloudbreak.domain.FileSystem

@EntityType(entityClass = FileSystem::class)
interface FileSystemRepository : CrudRepository<FileSystem, Long>
