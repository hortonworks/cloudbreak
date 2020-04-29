package com.sequenceiq.redbeams.flow.redbeams.start.actions;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartContext;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartState;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import org.springframework.statemachine.StateContext;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

public abstract class AbstractRedbeamsStartAction<P extends Payload>
        extends AbstractAction<RedbeamsStartState, RedbeamsStartEvent, RedbeamsStartContext, P> {

    private static final String UNKOWN_VENDOR_DISPLAY_NAME = "UNKNOWN";

    @Inject
    private DBStackService dbStackService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

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
        Optional<DBStack> optionalDBStack = dbStackService.findById(payload.getResourceId());

        CloudContext cloudContext = null;
        CloudCredential cloudCredential = null;
        String dbInstanceIdentifier = null;
        String dbVendorDisplayName = UNKOWN_VENDOR_DISPLAY_NAME;

        if (optionalDBStack.isPresent()) {
            DBStack dbStack = optionalDBStack.get();
            MDCBuilder.buildMdcContext(dbStack);
            Location location = location(region(dbStack.getRegion()), availabilityZone(dbStack.getAvailabilityZone()));
            String userName = dbStack.getOwnerCrn().getUserId();
            String accountId = dbStack.getOwnerCrn().getAccountId();
            cloudContext = new CloudContext(dbStack.getId(), dbStack.getName(), dbStack.getCloudPlatform(), dbStack.getPlatformVariant(),
                    location, userName, accountId);
            Credential credential = credentialService.getCredentialByEnvCrn(dbStack.getEnvironmentId());
            cloudCredential = credentialConverter.convert(credential);

            if (dbStack.getDatabaseServer() != null) {
                dbInstanceIdentifier = dbStack.getDatabaseServer().getName();
                if (dbStack.getDatabaseServer().getDatabaseVendor() != null) {
                    dbVendorDisplayName = dbStack.getDatabaseServer().getDatabaseVendor().displayName();
                }
            }
        }
        return new RedbeamsStartContext(flowParameters, cloudContext, cloudCredential, dbInstanceIdentifier, dbVendorDisplayName);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<RedbeamsStartContext> flowContext, Exception ex) {
        return new RedbeamsFailureEvent(payload.getResourceId(), ex);
    }
}
