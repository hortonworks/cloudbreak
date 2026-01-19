package com.sequenceiq.it.cloudbreak.microservice;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.freeipa.api.client.FreeIpaApiKeyClient;
import com.sequenceiq.freeipa.api.client.FreeIpaApiUserCrnClient;
import com.sequenceiq.freeipa.api.client.FreeIpaApiUserCrnClientBuilder;
import com.sequenceiq.freeipa.api.client.FreeIpaApiUserCrnEndpoint;
import com.sequenceiq.freeipa.api.client.FreeipaInternalCrnClient;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.UserSyncState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.EnvironmentAware;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaChildEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaDownscaleTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaHealthDetailsDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaOperationStatusTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaRotationTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTrustCommandsDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUpscaleTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncStatusDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeipaChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeipaUsedImagesTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.freeipa.FreeIpaOperationWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.freeipa.FreeIpaUserSyncWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.freeipa.FreeIpaWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.InstanceWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.freeipa.FreeIpaInstanceWaitObject;

public class FreeIpaClient<E extends Enum<E>> extends MicroserviceClient<com.sequenceiq.freeipa.api.client.FreeIpaClient, FreeIpaApiUserCrnEndpoint, E,
        WaitObject> {
    private com.sequenceiq.freeipa.api.client.FreeIpaClient freeIpaClient;

    private FreeipaInternalCrnClient freeipaInternalCrnClient;

    private com.sequenceiq.freeipa.api.client.FreeIpaClient alternativeFreeIpaClient;

    private FreeipaInternalCrnClient alternativeFreeipaInternalCrnClient;

    public FreeIpaClient(CloudbreakUser cloudbreakUser, RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator,
            String freeipaAddress, String freeipaInternalAddress, String alternativeFreeipaAddress, String alternativeFreeipaInternalAddress) {
        setActing(cloudbreakUser);
        freeIpaClient = createFreeIpaClient(freeipaAddress, cloudbreakUser);
        freeipaInternalCrnClient = createFreeipaInternalClient(freeipaInternalAddress, regionAwareInternalCrnGenerator);

        if (isNotEmpty(alternativeFreeipaAddress) && isNotEmpty(alternativeFreeipaInternalAddress)) {
            alternativeFreeIpaClient = createFreeIpaClient(alternativeFreeipaAddress, cloudbreakUser);
            alternativeFreeipaInternalCrnClient = createFreeipaInternalClient(alternativeFreeipaInternalAddress, regionAwareInternalCrnGenerator);
        }
    }

    private com.sequenceiq.freeipa.api.client.FreeIpaClient createFreeIpaClient(String address, CloudbreakUser cloudbreakUser) {
        return new FreeIpaApiKeyClient(address, new ConfigKey(false, true, true, TIMEOUT))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint(TestContext testContext) {
        if (testContext.shouldUseAlternativeEndpoints()) {
            return alternativeFreeIpaClient.getFlowPublicEndpoint();
        } else {
            return freeIpaClient.getFlowPublicEndpoint();
        }
    }

    @Override
    public WaitObject waitObject(CloudbreakTestDto entity, String name, Map<String, E> desiredStatuses,
            TestContext testContext, Set<E> ignoredFailedStatuses) {
        if (entity instanceof FreeIpaUserSyncTestDto) {
            FreeIpaUserSyncTestDto freeIpaSyncTestDto = (FreeIpaUserSyncTestDto) entity;
            if (freeIpaSyncTestDto.getOperationId() == null ||
                    (Crn.isCrn(freeIpaSyncTestDto.getOperationId())
                            && CrnResourceDescriptor.USERSYNC.checkIfCrnMatches(Crn.safeFromString(freeIpaSyncTestDto.getOperationId())))) {
                E desiredStatus = desiredStatuses.get("status");
                UserSyncState desiredUserSyncState = getDesiredUserSyncState(desiredStatus);
                return new FreeIpaUserSyncWaitObject(this, freeIpaSyncTestDto.getName(), freeIpaSyncTestDto.getEnvironmentCrn(),
                        desiredUserSyncState, (Set<UserSyncState>) ignoredFailedStatuses, testContext);
            } else {
                return new FreeIpaOperationWaitObject(this, freeIpaSyncTestDto.getOperationId(), freeIpaSyncTestDto.getName(),
                        freeIpaSyncTestDto.getEnvironmentCrn(), (OperationState) desiredStatuses.get("status"), (Set<OperationState>) ignoredFailedStatuses,
                        testContext);
            }
        } else if (entity instanceof FreeIpaOperationStatusTestDto) {
            FreeIpaOperationStatusTestDto testDto = (FreeIpaOperationStatusTestDto) entity;
            return new FreeIpaOperationWaitObject(this, testDto.getOperationId(), testDto.getName(),
                    testContext.get(EnvironmentTestDto.class).getResponse().getCrn(), (OperationState) desiredStatuses.get("status"),
                    (Set<OperationState>) ignoredFailedStatuses, testContext);
        } else {
            EnvironmentAware environmentAware = (EnvironmentAware) entity;
            return new FreeIpaWaitObject(this, entity.getName(), environmentAware.getEnvironmentCrn(), (Status) desiredStatuses.get("status"),
                    (Set<Status>) ignoredFailedStatuses, testContext);
        }
    }

    private <E extends Enum<E>> UserSyncState getDesiredUserSyncState(E desiredStatus) {
        UserSyncState desiredUserSyncState;
        if (desiredStatus instanceof OperationState operationState) {
            desiredUserSyncState = switch (operationState) {
                case REQUESTED, RUNNING -> UserSyncState.SYNC_IN_PROGRESS;
                case COMPLETED -> UserSyncState.UP_TO_DATE;
                case FAILED, TIMEDOUT, REJECTED -> UserSyncState.SYNC_FAILED;
            };
        } else {
            desiredUserSyncState = (UserSyncState) desiredStatus;
        }
        return desiredUserSyncState;
    }

    @Override
    public com.sequenceiq.freeipa.api.client.FreeIpaClient getDefaultClient(TestContext testContext) {
        if (testContext.shouldUseAlternativeEndpoints()) {
            return alternativeFreeIpaClient;
        } else {
            return freeIpaClient;
        }
    }

    private FreeipaInternalCrnClient createFreeipaInternalClient(String serverRoot,
            RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator) {
        FreeIpaApiUserCrnClient freeIpaApiUserCrnClient = new FreeIpaApiUserCrnClientBuilder(serverRoot)
                .withCertificateValidation(false)
                .withIgnorePreValidation(true)
                .withDebug(true)
                .build();
        return new FreeipaInternalCrnClient(freeIpaApiUserCrnClient, regionAwareInternalCrnGenerator);
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(FreeIpaTestDto.class.getSimpleName(),
                FreeIpaUserSyncTestDto.class.getSimpleName(),
                LdapTestDto.class.getSimpleName(),
                FreeIpaChildEnvironmentTestDto.class.getSimpleName(),
                KerberosTestDto.class.getSimpleName(),
                FreeIpaUserSyncStatusDto.class.getSimpleName(),
                FreeipaUsedImagesTestDto.class.getSimpleName(),
                FreeIpaOperationStatusTestDto.class.getSimpleName(),
                FreeipaChangeImageCatalogTestDto.class.getSimpleName(),
                FreeIpaUpscaleTestDto.class.getSimpleName(),
                FreeIpaRotationTestDto.class.getSimpleName(),
                FreeIpaDownscaleTestDto.class.getSimpleName(),
                FreeIpaHealthDetailsDto.class.getSimpleName(),
                FreeIpaTrustCommandsDto.class.getSimpleName());
    }

    @Override
    public FreeIpaApiUserCrnEndpoint getInternalClient(TestContext testContext) {
        checkIfInternalClientAllowed(testContext);
        FreeipaInternalCrnClient client;
        if (testContext.shouldUseAlternativeEndpoints()) {
            client = alternativeFreeipaInternalCrnClient;
        } else {
            client = freeipaInternalCrnClient;
        }
        return client.withInternalCrn();
    }

    @Override
    public <O extends Enum<O>> InstanceWaitObject waitInstancesObject(CloudbreakTestDto entity, TestContext testContext,
            List<String> instanceIds, O instanceStatus) {
        return new FreeIpaInstanceWaitObject(testContext, ((FreeIpaTestDto) entity).getResponse().getEnvironmentCrn(), instanceIds,
                (InstanceStatus) instanceStatus);
    }

}
