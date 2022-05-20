package com.sequenceiq.distrox.v1.distrox.service;

import static java.lang.String.format;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.saas.sdx.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.saas.sdx.status.StatusCheckResult;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToStackV4RequestConverter;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Service
public class DistroXService {

    @Inject
    private StackOperations stackOperations;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private DistroXV1RequestToStackV4RequestConverter stackRequestConverter;

    @Inject
    private FreeipaClientService freeipaClientService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    public StackV4Response post(DistroXV1Request request) {
        validate(request);
        return stackOperations.post(
                workspaceService.getForCurrentUser().getId(),
                restRequestThreadLocalService.getCloudbreakUser(),
                stackRequestConverter.convert(request),
                true);
    }

    private void validate(DistroXV1Request request) {
        DetailedEnvironmentResponse environment = Optional.ofNullable(environmentClientService.getByName(request.getEnvironmentName()))
                .orElseThrow(() -> new BadRequestException("No environment name provided hence unable to obtain some important data"));
        if (environment == null) {
            throw new BadRequestException(format("'%s' Environment does not exist.", request.getEnvironmentName()));
        }
        DescribeFreeIpaResponse freeipa = freeipaClientService.getByEnvironmentCrn(environment.getCrn());
        if (freeipa == null || freeipa.getAvailabilityStatus() == null || !freeipa.getAvailabilityStatus().isAvailable()) {
            throw new BadRequestException(format("If you want to provision a Data Hub then the FreeIPA instance must be running in the '%s' Environment.",
                    environment.getName()));
        }
        Set<String> sdxCrns = platformAwareSdxConnector.listSdxCrns(environment.getName(), environment.getCrn());
        if (sdxCrns.isEmpty()) {
            throw new BadRequestException(format("Data Lake stack cannot be found for environment CRN: %s (%s)",
                    environment.getName(), environment.getCrn()));
        }
        Set<Pair<String, StatusCheckResult>> sdxCrnsWithAvailability = platformAwareSdxConnector.listSdxCrnsWithAvailability(environment.getName(),
                environment.getCrn(), sdxCrns);
        if (!sdxCrnsWithAvailability.stream().map(Pair::getValue).allMatch(statusCheckResult -> StatusCheckResult.AVAILABLE.equals(statusCheckResult))) {
            throw new BadRequestException("Data Lake stacks of environment should be available.");
        }
    }

}
