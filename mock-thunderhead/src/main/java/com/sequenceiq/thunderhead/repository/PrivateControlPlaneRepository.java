package com.sequenceiq.thunderhead.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.thunderhead.entity.PrivateControlPlane;

@Repository
public interface PrivateControlPlaneRepository extends JpaRepository<PrivateControlPlane, String> {

}
