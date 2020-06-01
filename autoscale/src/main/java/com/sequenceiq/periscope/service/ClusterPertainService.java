package com.sequenceiq.periscope.service;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.repository.ClusterPertainRepository;

@Service
public class ClusterPertainService {

    @Inject
    private ClusterPertainRepository clusterPertainRepository;

    public Optional<ClusterPertain> getClusterPertain(String clusterUserCrn) {
        return clusterPertainRepository.findByUserCrn(clusterUserCrn);
    }
}
