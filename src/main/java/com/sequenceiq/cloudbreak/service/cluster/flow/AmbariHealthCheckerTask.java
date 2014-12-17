package com.sequenceiq.cloudbreak.service.cluster.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;

@Component
@Scope("prototype")
public class AmbariHealthCheckerTask implements StatusCheckerTask<AmbariHealthCheckerTaskPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariHealthCheckerTask.class);

    @Autowired
    private StackRepository stackRepository;

    @Override
    public boolean checkStatus(AmbariHealthCheckerTaskPollerObject ambariHealthCheckerTaskPollerObject) {
        try {
            String ambariHealth = ambariHealthCheckerTaskPollerObject.getAmbariClient().healthCheck();
            if ("RUNNING".equals(ambariHealth)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.info("Ambari is not running yet, polling");
            return false;
        }
    }

    @Override
    public void handleTimeout(AmbariHealthCheckerTaskPollerObject t) {
        throw new InternalServerException(String.format("Operation timed out. Ambari server could not start %s", t.getAmbariClient().getAmbari().getUri()));
    }

    @Override
    public boolean exitPoller(AmbariHealthCheckerTaskPollerObject ambariHealthCheckerTaskPollerObject) {
        try {
            stackRepository.findById(ambariHealthCheckerTaskPollerObject.getStack().getId());
            return false;
        } catch (Exception ex) {
            return true;
        }
    }

    @Override
    public String successMessage(AmbariHealthCheckerTaskPollerObject t) {
        return String.format("Ambari server successfully started '%s'", t.getAmbariClient().getAmbari().getUri());
    }

}
