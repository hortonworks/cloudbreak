package com.sequenceiq.redbeams.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.redbeams.domain.stack.DBResource;

@EntityType(entityClass = DBResource.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface DBResourceRepository extends JpaRepository<DBResource, Long> {

    @Query("SELECT r FROM DBResource r WHERE r.dbStack.id = :stackId AND r.resourceName = :name AND r.resourceType = :type")
    Optional<DBResource> findByStackIdAndNameAndType(@Param("stackId") Long stackId, @Param("name") String name, @Param("type") ResourceType type);

    @Query("SELECT r FROM DBResource r WHERE r.dbStack.id = :stackId")
    List<DBResource> findAllByStackId(@Param("stackId") long stackId);

}
