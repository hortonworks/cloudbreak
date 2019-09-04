package com.sequenceiq.cloudbreak.service.stack;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.repository.StackViewRepository;

@Service
public class StackViewService {

    @Inject
    private StackViewRepository stackViewRepository;

    public Optional<StackView> findById(Long id) {
        return stackViewRepository.findById(id);
    }

    public Optional<StackView> findByName(String name, Long workspaceId) {
        return stackViewRepository.findByNameAndWorkspaceId(name, workspaceId);
    }

    public Optional<StackView> findNotTerminatedByName(String name, Long workspaceId) {
        return stackViewRepository.findByWorkspaceIdAndName(workspaceId, name);
    }

    public Optional<StackView> findNotTerminatedByCrn(String crn, Long workspaceId) {
        return stackViewRepository.findByWorkspaceIdAndCrn(workspaceId, crn);
    }

    public Set<StackView> findNotTerminatedByCrnList(List<String> crns, Long workspaceId) {
        return stackViewRepository.findByWorkspaceIdAndCrns(workspaceId, crns);
    }

    public Set<StackView> findNotTerminatedByNameList(List<String> names, Long workspaceId) {
        return stackViewRepository.findByWorkspaceIdAndNames(workspaceId, names);
    }

}
