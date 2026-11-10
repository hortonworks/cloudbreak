package com.sequenceiq.redbeams.flow.redbeams.common;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

/**
 * Builds the {@link RedbeamsContext} shared by the redbeams flow actions from a {@link DBStack} id. Extracted so the identical
 * cloud context, credential and database stack assembly does not have to be repeated in every abstract flow action.
 */
@Component
public class RedbeamsFlowContextBuilder {

    @Inject
    private DBStackService dbStackService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private DBStackToDatabaseStackConverter databaseStackConverter;

    public RedbeamsContext createRedbeamsContext(FlowParameters flowParameters, Long resourceId) {
        DBStack dbStack = dbStackService.getById(resourceId);
        MDCBuilder.buildMdcContext(dbStack);
        Location location = location(region(dbStack.getRegion()), availabilityZone(dbStack.getAvailabilityZone()));
        String accountId = dbStack.getOwnerCrn().getAccountId();
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(dbStack.getId())
                .withName(dbStack.getName())
                .withCrn(dbStack.getResourceCrn())
                .withPlatform(dbStack.getCloudPlatform())
                .withVariant(dbStack.getPlatformVariant())
                .withLocation(location)
                .withUserName(dbStack.getUserName())
                .withAccountId(accountId)
                .build();
        Credential credential = credentialService.getCredentialByEnvCrn(dbStack.getEnvironmentId());
        CloudCredential cloudCredential = credentialConverter.convert(credential);
        DatabaseStack databaseStack = databaseStackConverter.convert(dbStack);
        return new RedbeamsContext(flowParameters, cloudContext, cloudCredential, databaseStack, dbStack);
    }
}
