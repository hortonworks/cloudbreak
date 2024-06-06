package com.sequenceiq.thunderhead.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.thunderhead.entity.Cdl;

@Repository
public interface CdlRespository extends CrudRepository<Cdl, String> {

    Optional<Cdl> findByCrn(String crn);

    Optional<Cdl> findByEnvironmentCrn(String environmentCrn);
}
