package com.sequenceiq.redbeams.controller.v4.database;

//import javax.transaction.Transactional;
//import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Component;
//import org.springframework.stereotype.Controller;

//import com.sequenceiq.cloudbreak.controller.v4.NotificationController;
import com.sequenceiq.redbeams.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.EchoResponse;
//import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

//@Controller
//@Transactional(TxType.NEVER)
//@WorkspaceEntityType(RDSConfig.class)
@Component
public class DatabaseV4Controller /*extends NotificationController */implements DatabaseV4Endpoint {
    @Override
    public EchoResponse echo(String message) {
        return new EchoResponse(message);
    }
}
