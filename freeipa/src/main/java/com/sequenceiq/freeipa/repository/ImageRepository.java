package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.freeipa.entity.Image;
import com.sequenceiq.freeipa.entity.Stack;

@Transactional(Transactional.TxType.REQUIRED)
public interface ImageRepository extends CrudRepository<Image, Long> {

    Image getByStack(Stack stack);
}
