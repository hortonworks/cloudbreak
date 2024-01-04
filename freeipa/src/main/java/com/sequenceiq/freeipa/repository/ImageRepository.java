package com.sequenceiq.freeipa.repository;

import java.util.List;

import jakarta.transaction.Transactional;

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

    @Query("SELECT i FROM image i " +
            "LEFT JOIN i.stack s " +
            "WHERE (s.terminated = -1 OR s.terminated >= :thresholdTimestamp)")
    List<ImageEntity> findImagesOfAliveStacks(@Param("thresholdTimestamp") long thresholdTimestamp);
}
