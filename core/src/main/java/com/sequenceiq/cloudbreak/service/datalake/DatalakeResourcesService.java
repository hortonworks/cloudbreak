package com.sequenceiq.cloudbreak.service.datalake;

import java.util.Set;

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

    public Set<String> findDatalakeResourcesNamesByWorkspaceAndEnvironment(Long workspaceId, Long envId) {
        return datalakeResourcesRepository.findDatalakeResourcesNamesByWorkspaceAndEnvironment(workspaceId, envId);
    }
}
