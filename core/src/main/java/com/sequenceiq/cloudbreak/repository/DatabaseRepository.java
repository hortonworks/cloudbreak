package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@Repository
@Transactional(Transactional.TxType.REQUIRED)
@EntityType(entityClass = Database.class)
public interface DatabaseRepository extends CrudRepository<Database, Long> {
    @Modifying
    @Query("UPDATE Database d SET d.externalDatabaseEngineVersion = :externalDatabaseEngineVersion WHERE d.id = :id")
    int updateExternalDatabaseEngineVersion(@Param("id") Long id, @Param("externalDatabaseEngineVersion") String externalDatabaseEngineVersion);
}
