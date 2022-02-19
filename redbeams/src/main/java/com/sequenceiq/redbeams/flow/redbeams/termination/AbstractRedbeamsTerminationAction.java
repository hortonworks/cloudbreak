package com.sequenceiq.redbeams.flow.redbeams.termination;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate.TerminateDatabaseServerFailed;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

public abstract class AbstractRedbeamsTerminationAction<P extends RedbeamsEvent>
        extends AbstractAction<RedbeamsTerminationState, RedbeamsTerminationEvent, RedbeamsContext, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRedbeamsTerminationAction.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private DBStackToDatabaseStackConverter databaseStackConverter;

    private boolean failOnMissingDBStack;

    protected AbstractRedbeamsTerminationAction(Class<P> payloadClass) {
        this(payloadClass, true);
    }

    protected AbstractRedbeamsTerminationAction(Class<P> payloadClass, boolean failOnMissingDBStack) {
        super(payloadClass);
        this.failOnMissingDBStack = failOnMissingDBStack;
    }

    @PostConstruct
    public void init() {
        super.init();
    }

    @Override
    protected void doExecute(RedbeamsContext context, P payload, Map<Object, Object> variables) throws Exception {
        if (failOnMissingDBStack && !context.doesDBStackExist()) {
            throw new RedbeamsException(String.format("The dbstack for %s id does not exist!", payload.getResourceId()));
        }
        sendEvent(context);
    }

    @Override
    protected RedbeamsContext createFlowContext(FlowParameters flowParameters,
            StateContext<RedbeamsTerminationState, RedbeamsTerminationEvent> stateContext, P payload) {
        Optional<DBStack> optionalDBStack = dbStackService.findById(payload.getResourceId());
        CloudContext cloudContext = null;
        CloudCredential cloudCredential = null;
        DatabaseStack databaseStack = null;
        DBStack dbStack = null;
        if (optionalDBStack.isPresent()) {
            dbStack = optionalDBStack.get();
            MDCBuilder.buildMdcContext(dbStack);
            Location location = location(region(dbStack.getRegion()), availabilityZone(dbStack.getAvailabilityZone()));
            String userName = dbStack.getOwnerCrn().getUserId();
            String accountId = dbStack.getOwnerCrn().getAccountId();
            cloudContext = CloudContext.Builder.builder()
                    .withId(dbStack.getId())
                    .withName(dbStack.getName())
                    .withCrn(dbStack.getResourceCrn())
                    .withPlatform(dbStack.getCloudPlatform())
                    .withVariant(dbStack.getPlatformVariant())
                    .withUserName(userName)
                    .withLocation(location)
                    .withAccountId(accountId)
                    .build();
            try {
                Credential credential = credentialService.getCredentialByEnvCrn(dbStack.getEnvironmentId());
                cloudCredential = credentialConverter.convert(credential);
            } catch (Exception ex) {
                LOGGER.warn("Could not detect credential for environment: {}", dbStack.getEnvironmentId());
            }
            databaseStack = databaseStackConverter.convert(dbStack);
        } else {
            LOGGER.warn("DBStack for {} id is not found in the database, it seems to be only possible if the redbeams process was killed during the execution" +
                    " of the termination finished action", payload.getResourceId());
        }
        return new RedbeamsContext(flowParameters, cloudContext, cloudCredential, databaseStack, dbStack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<RedbeamsContext> flowContext, Exception ex) {
        return new TerminateDatabaseServerFailed(payload.getResourceId(), ex, payload.isForced());
    }
}
