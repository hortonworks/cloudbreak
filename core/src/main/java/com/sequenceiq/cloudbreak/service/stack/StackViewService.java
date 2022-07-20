package com.sequenceiq.cloudbreak.service.stack;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.repository.StackViewRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;

@Service("stackViewServiceDeprecated")
@Deprecated
public class StackViewService {

    @Inject
    private StackViewRepository stackViewRepository;

    public StackView getById(Long id) {
        return stackViewRepository.findById(id).orElseThrow(() -> new CloudbreakRuntimeException("Can not find stackview by stack id: " + id));
    }

    public Optional<StackView> findById(Long id) {
        return stackViewRepository.findById(id);
    }

    public Optional<StackView> findByName(String name, Long workspaceId) {
        return stackViewRepository.findByNameAndWorkspaceId(name, workspaceId);
    }

    public Optional<StackView> findNotTerminatedByName(String name, Long workspaceId) {
        return stackViewRepository.findByWorkspaceIdAndName(workspaceId, name);
    }

    public Set<StackView> findNotTerminatedByNames(List<String> names, Long workspaceId) {
        return stackViewRepository.findByWorkspaceIdAndNames(workspaceId, names);
    }

    public Optional<StackView> findNotTerminatedByCrn(String crn, Long workspaceId) {
        return stackViewRepository.findNotTerminatedByWorkspaceIdAndCrn(workspaceId, crn);
    }

    public Optional<StackView> findByCrn(String crn, Long workspaceId) {
        return stackViewRepository.findByWorkspaceIdAndCrn(workspaceId, crn);
    }

    public Set<StackView> findNotTerminatedByCrns(Collection<String> crns, Long workspaceId) {
        return stackViewRepository.findByWorkspaceIdAndCrns(workspaceId, crns);
    }

    public Optional<String> findResourceCrnByNameAndTenantName(String name, String tenantName) {
        return stackViewRepository.findResourceCrnByTenantNameAndName(tenantName, name);
    }

    public Set<String> findResourceCrnsByNameListAndTenant(List<String> names, String tenantName) {
        return stackViewRepository.findResourceCrnsByTenantNameAndNames(tenantName, names);
    }

    public Set<String> findResourceCrnsByTenant(String tenantName) {
        return stackViewRepository.findResourceCrnsByTenant(tenantName);
    }

    public Optional<StackView> findDatalakeViewByEnvironmentCrn(String environmentCrn) {
        List<StackView> result = stackViewRepository.findDatalakeViewByEnvironmentCrnOrderedByCreationTime(environmentCrn);
        return  result.isEmpty() ? Optional.empty()
                : Optional.of(stackViewRepository.findDatalakeViewByEnvironmentCrnOrderedByCreationTime(environmentCrn).get(0));
    }

}
