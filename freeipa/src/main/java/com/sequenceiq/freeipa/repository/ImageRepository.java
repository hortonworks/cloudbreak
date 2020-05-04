package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;

@Transactional(Transactional.TxType.REQUIRED)
public interface ImageRepository extends CrudRepository<ImageEntity, Long> {

    ImageEntity getByStack(Stack stack);

    @Query("SELECT i FROM image i " +
            "LEFT JOIN i.stack s " +
            "WHERE s.id = :stackId")
    ImageEntity getByStackId(@Param("stackId") Long stackId);
}
