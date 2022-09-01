package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.freeipa.entity.Template;

@Transactional(TxType.REQUIRED)
public interface TemplateRepository extends CrudRepository<Template, Long> {

}