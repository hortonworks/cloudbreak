package com.sequenceiq.freeipa.service.rotation.dbuscredential.executor;

import java.util.function.Function;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.model.CdpAccessKeyType;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep;
import com.sequenceiq.freeipa.service.AltusMachineUserService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.telemetry.TelemetryConfigService;

@Component
public class UmsDatabusCredentialRotationExecutor extends AbstractRotationExecutor<RotationContext> {

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Inject
    private StackService stackService;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @Inject
    private TelemetryConfigService telemetryConfigService;

    @Override
    protected void rotate(RotationContext rotationContext) throws Exception {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(rotationContext.getResourceCrn(), ThreadBasedUserCrnProvider.getAccountId());
        if (stack.getDatabusCredential() != null) {
            DataBusCredential currentDatabusCredential = new Json(stack.getDatabusCredential()).get(DataBusCredential.class);
            AltusCredential newAltusCredential = grpcUmsClient.generateAccessSecretKeyPair(ThreadBasedUserCrnProvider.getAccountId(),
                    currentDatabusCredential.getMachineUserName(),
                    UserManagementProto.AccessKeyType.Value.valueOf(currentDatabusCredential.getAccessKeyType().toUpperCase()));
            DataBusCredential newDataBusCredential = altusMachineUserService.getDataBusCredential(newAltusCredential, stack,
                    CdpAccessKeyType.valueOf(currentDatabusCredential.getAccessKeyType().toUpperCase()));
            uncachedSecretServiceForRotation.putRotation(stack.getDatabusCredentialSecret().getSecret(), new Json(newDataBusCredential).getValue());
        } else {
            altusMachineUserService.getOrCreateDataBusCredentialIfNeeded(stack, telemetryConfigService.getCdpAccessKeyType(stack));
        }
    }

    @Override
    protected void rollback(RotationContext rotationContext) throws Exception {
        removeCredential(rotationContext.getResourceCrn(), RotationSecret::getSecret, RotationSecret::getBackupSecret);
    }

    @Override
    protected void finalizeRotation(RotationContext rotationContext) throws Exception {
        removeCredential(rotationContext.getResourceCrn(), RotationSecret::getBackupSecret, RotationSecret::getSecret);
    }

    private void removeCredential(String crn, Function<RotationSecret, String> removeableSecretStringProviderFunction,
            Function<RotationSecret, String> newVaultStringProviderFunction) throws Exception {
        Crn environmentCrn = Crn.safeFromString(crn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(crn, environmentCrn.getAccountId());
        String vaultPath = stack.getDatabusCredentialSecret().getSecret();
        RotationSecret rotationSecret = uncachedSecretServiceForRotation.getRotation(vaultPath);
        if (rotationSecret != null && rotationSecret.isRotation()) {
            DataBusCredential dataBusCredential = new Json(removeableSecretStringProviderFunction.apply(rotationSecret)).get(DataBusCredential.class);
            grpcUmsClient.deleteAccessKey(dataBusCredential.getAccessKey(), ThreadBasedUserCrnProvider.getAccountId());
            uncachedSecretServiceForRotation.update(vaultPath, newVaultStringProviderFunction.apply(rotationSecret));
        }
    }

    @Override
    protected void preValidate(RotationContext rotationContext) throws Exception {

    }

    @Override
    protected void postValidate(RotationContext rotationContext) throws Exception {

    }

    @Override
    protected Class<RotationContext> getContextClass() {
        return RotationContext.class;
    }

    @Override
    public SecretRotationStep getType() {
        return FreeIpaSecretRotationStep.FREEIPA_UMS_DATABUS_CREDENTIAL;
    }
}
