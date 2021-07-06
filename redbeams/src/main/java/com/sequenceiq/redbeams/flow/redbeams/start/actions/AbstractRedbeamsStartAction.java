package com.sequenceiq.redbeams.flow.redbeams.start.actions;

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
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartContext;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartState;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

public abstract class AbstractRedbeamsStartAction<P extends Payload>
        extends AbstractAction<RedbeamsStartState, RedbeamsStartEvent, RedbeamsStartContext, P> {

    @Inject
    private DBStackService dbStackService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private DBStackToDatabaseStackConverter databaseStackConverter;

    protected AbstractRedbeamsStartAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @PostConstruct
    public void init() {
        super.init();
    }

    @Override
    protected void doExecute(RedbeamsStartContext context, P payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context);
    }

    @Override
    protected RedbeamsStartContext createFlowContext(FlowParameters flowParameters,
            StateContext<RedbeamsStartState, RedbeamsStartEvent> stateContext, P payload) {
        DBStack dbStack = dbStackService.getById(payload.getResourceId());
        MDCBuilder.buildMdcContext(dbStack);
        Location location = location(region(dbStack.getRegion()), availabilityZone(dbStack.getAvailabilityZone()));
        String userName = dbStack.getOwnerCrn().getUserId();
        String accountId = dbStack.getOwnerCrn().getAccountId();
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(dbStack.getId())
                .withName(dbStack.getName())
                .withCrn(dbStack.getResourceCrn().toString())
                .withPlatform(dbStack.getCloudPlatform())
                .withVariant(dbStack.getPlatformVariant())
                .withLocation(location)
                .withUserName(userName)
                .withAccountId(accountId)
                .build();
        Credential credential = credentialService.getCredentialByEnvCrn(dbStack.getEnvironmentId());
        CloudCredential cloudCredential = credentialConverter.convert(credential);
        DatabaseStack databaseStack = databaseStackConverter.convert(dbStack);

        return new RedbeamsStartContext(flowParameters, cloudContext, cloudCredential, databaseStack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<RedbeamsStartContext> flowContext, Exception ex) {
        return new RedbeamsFailureEvent(payload.getResourceId(), ex);
    }
}
