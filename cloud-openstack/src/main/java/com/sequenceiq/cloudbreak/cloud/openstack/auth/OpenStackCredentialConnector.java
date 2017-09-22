package com.sequenceiq.cloudbreak.cloud.openstack.auth;

import static com.sequenceiq.cloudbreak.cloud.model.CloudCredential.SMART_SENSE_ID;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackSmartSenseIdGenerator;

@Service
public class OpenStackCredentialConnector implements CredentialConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackCredentialConnector.class);

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private OpenStackSmartSenseIdGenerator smartSenseIdGenerator;

    @Override
    public CloudCredentialStatus verify(AuthenticatedContext authenticatedContext) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        String smartSenseId = smartSenseIdGenerator.getSmartSenseId();
        if (StringUtils.isNoneEmpty(smartSenseId)) {
            credential.putParameter(SMART_SENSE_ID, smartSenseId);
        }
        return new CloudCredentialStatus(credential, CredentialStatus.VERIFIED);
    }

    @Override
    public CloudCredentialStatus create(AuthenticatedContext auth) {
        LOGGER.info("Create credential: {}", auth.getCloudCredential());
        return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.CREATED);
    }

    @Override
    public Map<String, String> interactiveLogin(CloudContext cloudContext, ExtendedCloudCredential extendedCloudCredential,
            CredentialNotifier credentialNotifier) {
        throw new UnsupportedOperationException("Interactive login not supported on Openstack");
    }

    @Override
    public CloudCredentialStatus delete(AuthenticatedContext auth) {
        LOGGER.info("Delete credential: {}", auth.getCloudCredential());
        return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.DELETED);
    }

}
