package com.sequenceiq.environment.environment.v1.cli;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;

@Component
public class DelegatingCliEnvironmentRequestConverter {

    private final List<EnvironmentRequestToCliRequestConverter> requestConverters;

    private final List<EnvironmentDtoToCliRequestConverter> dtoConverters;

    private final CredentialService credentialService;

    public DelegatingCliEnvironmentRequestConverter(List<EnvironmentRequestToCliRequestConverter> requestConverters,
            List<EnvironmentDtoToCliRequestConverter> dtoConverters,
            CredentialService credentialService) {
        this.requestConverters = requestConverters;
        this.dtoConverters = dtoConverters;
        this.credentialService = credentialService;
    }

    public Object convertRequest(EnvironmentRequest source) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String cloudPlatform = credentialService.getCloudPlatformByCredential(source.getCredentialName(), accountId);
        return requestConverters.stream()
                .filter(c -> c.supportedPlatform().name().equals(cloudPlatform))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No converter found for cloud platform: " + cloudPlatform))
                .convert(source);
    }

    public Object convertDto(EnvironmentDto source) {
        return dtoConverters.stream()
                .filter(c -> c.supportedPlatform().name().equals(source.getCloudPlatform()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No converter found for cloud platform: " + source.getCloudPlatform()))
                .convert(source);
    }

}
