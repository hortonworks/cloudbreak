package com.sequenceiq.environment.resourcepersister;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

@Transactional(TxType.REQUIRED)
public interface ResourceRepository extends CrudRepository<Resource, Long> {

    @Query("SELECT r FROM Resource r WHERE r.crn = :crn AND r.resourceName = :name AND r.resourceType = :type")
    Optional<Resource> findByStackIdAndNameAndType(@Param("crn") String crnId, @Param("name") String name,
            @Param("type") com.sequenceiq.environment.resourcepersister.ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.crn = :crn")
    List<Resource> findAllByStackId(@Param("crn") String crn);

}
