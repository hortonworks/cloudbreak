package com.sequenceiq.datalake.repository;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.datalake.entity.SdxDatabase;

@Repository
@Transactional(TxType.REQUIRED)
@EntityType(entityClass = SdxDatabase.class)
public interface SdxDatabaseRepository extends CrudRepository<SdxDatabase, Long> {
    @Modifying
    @Query("UPDATE SdxDatabase s SET s.databaseEngineVersion = :databaseEngineVersion WHERE s.id = :id")
    int updateDatabaseEngineVersion(@Param("id") Long id, @Param("databaseEngineVersion") String externalDatabaseEngineVersion);
}
