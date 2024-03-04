package com.sequenceiq.redbeams.flow.redbeams.start.actions;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.start.event.CertRotateInRedbeamsRequest;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerSuccess;

@Component("CERT_ROTATE_STATE")
public class CertRotateInRedbeamsAction extends AbstractRedbeamsStartAction<StartDatabaseServerSuccess> {

    public CertRotateInRedbeamsAction() {
        super(StartDatabaseServerSuccess.class);
    }

    @Override
    protected Selectable createRequest(RedbeamsContext context) {
        return new CertRotateInRedbeamsRequest(context.getCloudContext(), context.getCloudCredential(), context.getDatabaseStack());
    }
}
