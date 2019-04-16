package com.sequenceiq.cloudbreak.service.datalake;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.repository.cluster.DatalakeResourcesRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;

@Service
public class DatalakeResourcesService extends AbstractWorkspaceAwareResourceService<DatalakeResources> {

    @Inject
    private DatalakeResourcesRepository datalakeResourcesRepository;

    public Optional<DatalakeResources> findByDatalakeStackId(Long datalakeStackId) {
        return datalakeResourcesRepository.findByDatalakeStackId(datalakeStackId);
    }

    public Set<String> findDatalakeResourcesNamesByWorkspaceAndEnvironment(Long workspaceId, Long envId) {
        return datalakeResourcesRepository.findDatalakeResourcesNamesByWorkspaceAndEnvironment(workspaceId, envId);
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

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.STACK;
    }

    public Long countDatalakeResourcesInEnvironment(EnvironmentView environment) {
        return datalakeResourcesRepository.countDatalakeResourcesByEnvironment(environment);
    }

    public DatalakeResources save(DatalakeResources resources) {
        return datalakeResourcesRepository.save(resources);
    }

}
