package com.sequenceiq.cloudbreak.rotation.executor;

import java.util.function.Function;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.model.CdpAccessKeyType;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.service.altus.AltusMachineUserService;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;

@Service
public class UmsDatabusCredentialRotationExecutor extends AbstractRotationExecutor<RotationContext> {

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @Override
    protected void rotate(RotationContext rotationContext) throws Exception {
        StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
        DataBusCredential currentDatabusCredential = new Json(stackDto.getCluster().getDatabusCredential()).get(DataBusCredential.class);
        AltusCredential newAltusCredential = grpcUmsClient.generateAccessSecretKeyPair(ThreadBasedUserCrnProvider.getAccountId(),
                currentDatabusCredential.getMachineUserName(),
                UserManagementProto.AccessKeyType.Value.valueOf(currentDatabusCredential.getAccessKeyType().toUpperCase()));
        DataBusCredential newDataBusCredential = altusMachineUserService.getDataBusCredential(newAltusCredential, stackDto.getStack(),
                CdpAccessKeyType.valueOf(currentDatabusCredential.getAccessKeyType().toUpperCase()));
        uncachedSecretServiceForRotation.putRotation(stackDto.getCluster().getDatabusCredentialSecret().getSecret(), new Json(newDataBusCredential).getValue());
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
        StackDto stackDto = stackDtoService.getByCrn(crn);
        String vaultPath = stackDto.getCluster().getDatabusCredentialSecret().getSecret();
        RotationSecret rotationSecret = uncachedSecretServiceForRotation.getRotation(vaultPath);
        DataBusCredential dataBusCredential = new Json(removeableSecretStringProviderFunction.apply(rotationSecret)).get(DataBusCredential.class);
        grpcUmsClient.deleteAccessKey(dataBusCredential.getAccessKey(), ThreadBasedUserCrnProvider.getAccountId());
        uncachedSecretServiceForRotation.update(vaultPath, newVaultStringProviderFunction.apply(rotationSecret));
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
        return CloudbreakSecretRotationStep.UMS_DATABUS_CREDENTIAL;
    }
}
