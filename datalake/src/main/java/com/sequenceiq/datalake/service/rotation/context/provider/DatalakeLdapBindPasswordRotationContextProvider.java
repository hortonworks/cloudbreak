package com.sequenceiq.datalake.service.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_LDAP_BIND_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@Component
public class DatalakeLdapBindPasswordRotationContextProvider implements RotationContextProvider {
    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> context = new HashMap<>();
        context.put(CLOUDBREAK_ROTATE_POLLING, new PollerRotationContext(resourceCrn, CLUSTER_LDAP_BIND_PASSWORD));
        return context;
    }

    @Override
    public SecretType getSecret() {
        return DatalakeSecretType.DATALAKE_LDAP_BIND_PASSWORD;
    }
}
