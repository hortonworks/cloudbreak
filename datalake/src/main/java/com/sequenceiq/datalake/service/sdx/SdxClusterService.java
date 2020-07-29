package com.sequenceiq.datalake.service.sdx;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

@Service
public class SdxClusterService {

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    public Optional<SdxCluster> findById(Long id) {
        return sdxClusterRepository.findById(id);
    }
}
