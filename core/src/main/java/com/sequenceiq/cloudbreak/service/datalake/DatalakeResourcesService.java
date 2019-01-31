package com.sequenceiq.cloudbreak.service.datalake;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.repository.cluster.DatalakeResourcesRepository;

@Service
public class DatalakeResourcesService {

    @Inject
    private DatalakeResourcesRepository datalakeResourcesRepository;

    public Set<String> findDatalakeResourcesNamesByWorkspaceAndEnvironment(Long workspaceId, Long envId) {
        return datalakeResourcesRepository.findDatalakeResourcesNamesByWorkspaceAndEnvironment(workspaceId, envId);
    }

    public Optional<DatalakeResources> findByDatalakeStackId(Long stackId) {
        return Optional.ofNullable(datalakeResourcesRepository.findByDatalakeStackId(stackId));
    }

    public void delete(DatalakeResources datalakeResources) {
        datalakeResourcesRepository.delete(datalakeResources);
    }

    public DatalakeResources save(DatalakeResources resources) {
        return datalakeResourcesRepository.save(resources);
    }

    public Optional<DatalakeResources> findById(Long id) {
        return datalakeResourcesRepository.findById(id);
    }

}
