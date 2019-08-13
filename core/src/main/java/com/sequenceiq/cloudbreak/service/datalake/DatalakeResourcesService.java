package com.sequenceiq.cloudbreak.service.datalake;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.repository.cluster.DatalakeResourcesRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Service
public class DatalakeResourcesService extends AbstractWorkspaceAwareResourceService<DatalakeResources> {

    @Inject
    private DatalakeResourcesRepository datalakeResourcesRepository;

    public Optional<DatalakeResources> findByDatalakeStackId(Long datalakeStackId) {
        return datalakeResourcesRepository.findByDatalakeStackId(datalakeStackId);
    }

    public Set<String> findDatalakeResourcesNamesByWorkspaceAndEnvironment(Long workspaceId, String environmentCrn) {
        return datalakeResourcesRepository.findDatalakeResourcesNamesByWorkspaceAndEnvironment(workspaceId, environmentCrn);
    }

    public Set<DatalakeResources> findDatalakeResourcesByWorkspaceAndEnvironment(Long workspaceId, String environmentCrn) {
        return datalakeResourcesRepository.findDatalakeResourcesByWorkspaceAndEnvironment(workspaceId, environmentCrn);
    }

    public Optional<DatalakeResources> findById(Long datalakeResourceId) {
        return datalakeResourcesRepository.findById(datalakeResourceId);
    }

    @Override
    protected WorkspaceResourceRepository<DatalakeResources, Long> repository() {
        return datalakeResourcesRepository;
    }

    @Override
    protected void prepareDeletion(DatalakeResources resource) {

    }

    @Override
    protected void prepareCreation(DatalakeResources resource) {

    }

    public Long countDatalakeResourcesInEnvironment(String environmentCrn) {
        return datalakeResourcesRepository.countDatalakeResourcesByEnvironmentCrn(environmentCrn);
    }

    public DatalakeResources save(DatalakeResources resources) {
        return datalakeResourcesRepository.save(resources);
    }

}
