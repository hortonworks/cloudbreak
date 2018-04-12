package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.RepositoryConfigValidationEndpoint;
import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationRequest;
import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationResponse;
import com.sequenceiq.cloudbreak.service.cluster.RepositoryConfigValidationService;

@Controller
public class RepositoryConfigValidationController implements RepositoryConfigValidationEndpoint {

    @Inject
    private RepositoryConfigValidationService validationService;

    @Override
    public RepoConfigValidationResponse postRepositoryConfigValidationRequest(RepoConfigValidationRequest repoConfigValidationRequest) {
        return validationService.validate(repoConfigValidationRequest);
    }
}
