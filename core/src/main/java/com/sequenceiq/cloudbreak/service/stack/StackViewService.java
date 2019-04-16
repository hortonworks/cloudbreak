package com.sequenceiq.cloudbreak.service.stack;

import java.util.Optional;

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

}
