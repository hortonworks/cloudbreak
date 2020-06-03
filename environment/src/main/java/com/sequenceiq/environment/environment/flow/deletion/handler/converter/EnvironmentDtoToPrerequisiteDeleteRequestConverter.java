package com.sequenceiq.environment.environment.flow.deletion.handler.converter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.prerequisite.EnvironmentPrerequisiteDeleteRequest;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;

@Component
public class EnvironmentDtoToPrerequisiteDeleteRequestConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDtoToPrerequisiteDeleteRequestConverter.class);

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final Map<String, EnvironmentPrerequisiteDeleteRequestParameterSetter> environmentPrerequisiteDeleteRequestParameterSetters;

    public EnvironmentDtoToPrerequisiteDeleteRequestConverter(
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter,
            List<EnvironmentPrerequisiteDeleteRequestParameterSetter> environmentPrerequisiteDeleteRequestParameterSetterList) {
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        environmentPrerequisiteDeleteRequestParameterSetters = environmentPrerequisiteDeleteRequestParameterSetterList.stream()
                .collect(Collectors.toMap(e -> e.getCloudPlatform(), e -> e));
    }

    public EnvironmentPrerequisiteDeleteRequest convert(EnvironmentDto environmentDto) {
        EnvironmentPrerequisiteDeleteRequest environmentPrerequisiteDeleteRequest = new EnvironmentPrerequisiteDeleteRequest(
                credentialToCloudCredentialConverter.convert(environmentDto.getCredential()));
        String cloudPlatform = environmentDto.getCloudPlatform();
        Optional.ofNullable(environmentPrerequisiteDeleteRequestParameterSetters.get(cloudPlatform))
                .ifPresentOrElse(
                        s -> s.setParameters(environmentPrerequisiteDeleteRequest, environmentDto),
                        () -> LOGGER.debug("No prerequisite setter defined for cloudplatform {}", cloudPlatform));
        return environmentPrerequisiteDeleteRequest;
    }
}
