package com.sequenceiq.redbeams.flow.redbeams.provision;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

public abstract class AbstractRedbeamsProvisionAction<P extends Payload>
        extends AbstractAction<RedbeamsProvisionState, RedbeamsProvisionEvent, RedbeamsContext, P> {

    @Inject
    private DBStackService dbStackService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private DBStackToDatabaseStackConverter databaseStackConverter;

    protected AbstractRedbeamsProvisionAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @PostConstruct
    @Override
    public void init() {
        super.init();
    }

    @Override
    protected void doExecute(RedbeamsContext context, P payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context);
    }

    @Override
    protected RedbeamsContext createFlowContext(FlowParameters flowParameters,
        StateContext<RedbeamsProvisionState, RedbeamsProvisionEvent> stateContext, P payload) {
        DBStack dbStack = dbStackService.getById(payload.getResourceId());
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

    @Override
    protected Object getFailurePayload(P payload, Optional<RedbeamsContext> flowContext, Exception ex) {
        return new RedbeamsFailureEvent(payload.getResourceId(), ex);
    }

}
