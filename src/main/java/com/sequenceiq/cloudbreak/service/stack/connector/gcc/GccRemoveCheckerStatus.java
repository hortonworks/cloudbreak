package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.AddInstancesFailedException;

@Component
public class GccRemoveCheckerStatus implements StatusCheckerTask<GccRemoveReadyPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccRemoveCheckerStatus.class);
    private static final int FINISHED = 100;
    private static final int NOT_FOUND = 404;

    @Override
    public boolean checkStatus(GccRemoveReadyPollerObject gccRemoveReadyPollerObject) {
        LOGGER.info("Checking status of remove '{}' on '{}' stack.",
                gccRemoveReadyPollerObject.getName(), gccRemoveReadyPollerObject.getStack().getId());
        GccTemplate gccTemplate = (GccTemplate) gccRemoveReadyPollerObject.getStack().getTemplate();
        GccCredential gccCredential = (GccCredential) gccRemoveReadyPollerObject.getStack().getCredential();
        try {
            Integer progress = gccRemoveReadyPollerObject.getCompute().zoneOperations()
                    .get(gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(), gccRemoveReadyPollerObject.getOperation().getName()).execute().getProgress();
            return (progress.intValue() != FINISHED) ? false : true;
        } catch (GoogleJsonResponseException ex) {
            return exceptionHandler(ex, gccRemoveReadyPollerObject);
        } catch (NullPointerException | IOException e) {
            return false;
        }
    }

    private boolean exceptionHandler(GoogleJsonResponseException ex, GccRemoveReadyPollerObject gccRemoveReadyPollerObject) {
        if (ex.getDetails().get("code").equals(NOT_FOUND)) {
            GccCredential gccCredential = (GccCredential) gccRemoveReadyPollerObject.getStack().getCredential();
            try {
                Integer progress = gccRemoveReadyPollerObject.getCompute().globalOperations()
                        .get(gccCredential.getProjectId(), gccRemoveReadyPollerObject.getOperation().getName())
                        .execute().getProgress();
                return (progress.intValue() != FINISHED) ? false : true;
            } catch (IOException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void handleTimeout(GccRemoveReadyPollerObject gccRemoveReadyPollerObject) {
        throw new AddInstancesFailedException(String.format(
                "Something went wrong. Remove of '%s' resource unsuccess in a reasonable timeframe on '%s' stack.",
                gccRemoveReadyPollerObject.getName(), gccRemoveReadyPollerObject.getStack().getId()));
    }

    @Override
    public String successMessage(GccRemoveReadyPollerObject gccRemoveReadyPollerObject) {
        return String.format("Gcc resource '%s' is removed success on '%s' stack",
                gccRemoveReadyPollerObject.getName(), gccRemoveReadyPollerObject.getStack().getId());
    }
}
