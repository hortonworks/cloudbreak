package com.sequenceiq.freeipa.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.freeipa.entity.ChildEnvironment;
import com.sequenceiq.freeipa.entity.Stack;

@Transactional(Transactional.TxType.REQUIRED)
public interface ChildEnvironmentRepository extends CrudRepository<ChildEnvironment, Long> {

    @Query("SELECT c.stack FROM ChildEnvironment c WHERE c.environmentCrn = :childEnvironmentCrn AND c.stack.accountId = :accountId AND c.stack.terminated = -1")
    Optional<Stack> findParentStackByChildEnvironmentCrn(@Param("childEnvironmentCrn") String childEnvironmentCrn, @Param("accountId") String accountId);

    @Query("SELECT c.stack FROM ChildEnvironment c WHERE c.environmentCrn = :childEnvironmentCrn AND c.stack.accountId = :accountId")
    List<Stack> findMultipleParentByEnvironmentCrnEvenIfTerminated(
            @Param("childEnvironmentCrn") String childEnvironmentCrn,
            @Param("accountId") String accountId);

    @Query("SELECT c.stack FROM ChildEnvironment c "
            + "LEFT JOIN FETCH c.stack.instanceGroups ig "
            + "LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE c.environmentCrn = :childEnvironmentCrn AND c.stack.accountId = :accountId")
    List<Stack> findMultipleParentByEnvironmentCrnEvenIfTerminatedWithList(
            @Param("childEnvironmentCrn") String childEnvironmentCrn,
            @Param("accountId") String accountId);

    @Query("SELECT c.stack FROM ChildEnvironment c LEFT JOIN FETCH c.stack.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE c.environmentCrn = :childEnvironmentCrn AND c.stack.accountId = :accountId AND c.stack.resourceCrn = :resourceCrn")
    Optional<Stack> findParentByEnvironmentCrnAndCrnWthListsEvenIfTerminated(
            @Param("childEnvironmentCrn") String childEnvironmentCrn,
            @Param("accountId") String accountId,
            @Param("resourceCrn") String resourceCrn);

    @Query("SELECT c FROM ChildEnvironment c "
            + "WHERE c.environmentCrn = :childEnvironmentCrn AND c.stack.environmentCrn = :parentEnvironmentCrn AND c.stack.accountId = :accountId")
    Optional<ChildEnvironment> findByParentAndChildEnvironmentCrns(
            @Param("parentEnvironmentCrn") String parentEnvironmentCrn,
            @Param("childEnvironmentCrn") String childEnvironmentCrn,
            @Param("accountId") String accountId);

    @Query("SELECT c FROM ChildEnvironment c WHERE c.stack.id = :stackId AND c.stack.accountId = :accountId")
    List<ChildEnvironment> findByStackId(@Param("stackId") Long stackId, @Param("accountId") String accountId);
}
