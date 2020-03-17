package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.freeipa.entity.Image;
import com.sequenceiq.freeipa.entity.Stack;

@Transactional(Transactional.TxType.REQUIRED)
public interface ImageRepository extends CrudRepository<Image, Long> {

    Image getByStack(Stack stack);

    @Query("SELECT i FROM Image i " +
            "LEFT JOIN i.stack s " +
            "WHERE s.id = :stackId")
    Image getByStackId(@Param("stackId") Long stackId);
}
