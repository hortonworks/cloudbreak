package com.sequenceiq.cloudbreak.service.datalake;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.repository.cluster.DatalakeResourcesRepository;

@Service
public class DatalakeResourcesService {

    @Inject
    private DatalakeResourcesRepository datalakeResourcesRepository;

    public DatalakeResources getDatalakeResources(Long datalakeStackId) {
        return datalakeResourcesRepository.findByDatalakeStackId(datalakeStackId);
    }
}
