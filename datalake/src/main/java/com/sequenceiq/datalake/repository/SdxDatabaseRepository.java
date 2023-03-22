package com.sequenceiq.datalake.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.datalake.entity.SdxDatabase;

public interface SdxDatabaseRepository extends CrudRepository<SdxDatabase, Long> {
}
