package com.sequenceiq.thunderhead.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.thunderhead.entity.PublicCertGen;

@Repository
public interface PublicCertGenRespository extends CrudRepository<PublicCertGen, String> {

    Optional<PublicCertGen> findByWorkFlowId(String workFlowId);
}
