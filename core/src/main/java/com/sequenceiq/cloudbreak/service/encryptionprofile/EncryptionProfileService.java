package com.sequenceiq.cloudbreak.service.encryptionprofile;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class EncryptionProfileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionProfileService.class);

    @Inject
    private EnvironmentService environmentService;

    public EncryptionProfileResponse getEncryptionProfileByNameOrDefault(DetailedEnvironmentResponse environmentResponse, StackDto stackDto) {
        StackView stackView = stackDto.getStack();
        ClusterView clusterView = stackDto.getCluster();
        String encryptionProfileName;

        if (StringUtils.isNotBlank(clusterView.getEncryptionProfileName())) {
            encryptionProfileName = clusterView.getEncryptionProfileName();
        } else {
            encryptionProfileName = environmentResponse.getEncryptionProfileName();
        }

        return environmentService.getEncryptionProfileByNameOrDefaultIfEmpty(encryptionProfileName);
    }
}
