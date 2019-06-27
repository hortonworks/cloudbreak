package com.sequenceiq.redbeams.service.stack;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;

@Component
public class DBStackStatusUpdater {

    @Inject
    private DBStackService dbStackService;

    @Inject
    private Clock clock;

    public DBStack updateStatus(Long dbStackId, DetailedDBStackStatus detailedStatus) {
        return updateStatus(dbStackId, detailedStatus, "");
    }

    public DBStack updateStatus(Long dbStackId, DetailedDBStackStatus detailedStatus, String statusReason) {
        DBStack dbStack = dbStackService.getById(dbStackId);
        if (dbStack.getStatus() != Status.DELETE_COMPLETED) {
            Status status = detailedStatus.getStatus();
            DBStackStatus updatedStatus = new DBStackStatus(dbStack, status, statusReason, detailedStatus, clock.getCurrentTimeMillis());
            // The next line is a workaround to get the @OneToOne @MapsId relationship between DBStack and DBStackStatus working
            // see https://hibernate.atlassian.net/browse/HHH-12436
            // It might be removable once Spring Boot bumps up to Hibernate 5.4
            updatedStatus.setId(dbStackId);
            dbStack.setDBStackStatus(updatedStatus);
            dbStack = dbStackService.save(dbStack);
        }
        return dbStack;
    }

}
