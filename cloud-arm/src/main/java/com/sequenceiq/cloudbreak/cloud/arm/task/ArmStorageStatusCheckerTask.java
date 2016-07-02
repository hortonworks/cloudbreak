package com.sequenceiq.cloudbreak.cloud.arm.task;

import static com.sequenceiq.cloudbreak.cloud.arm.ArmUtils.NOT_FOUND;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient;
import com.sequenceiq.cloudbreak.cloud.arm.context.StorageCheckerContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

import groovyx.net.http.HttpResponseException;

@Component(ArmStorageStatusCheckerTask.NAME)
@Scope(value = "prototype")
public class ArmStorageStatusCheckerTask extends PollBooleanStateTask {
    public static final String NAME = "armStorageStatusCheckerTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmStorageStatusCheckerTask.class);

    @Inject
    private ArmClient armClient;
    private StorageCheckerContext storageCheckerContext;

    public ArmStorageStatusCheckerTask(AuthenticatedContext authenticatedContext, StorageCheckerContext storageCheckerContext) {
        super(authenticatedContext, true);
        this.storageCheckerContext = storageCheckerContext;
    }

    @Override
    public Boolean call() {
        AzureRMClient client = armClient.getClient(storageCheckerContext.getArmCredentialView());
        StorageStatus status = StorageStatus.OTHER;
        try {
            String storageStatus = client.getStorageStatus(storageCheckerContext.getGroupName(), storageCheckerContext.getStorageName());
            if (StorageStatus.SUCCEEDED.getValue().equals(storageStatus)) {
                status = StorageStatus.SUCCEEDED;
            }
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == NOT_FOUND) {
                status = StorageStatus.NOTFOUND;
            } else {
                LOGGER.warn("HttpResponseException occured: {}", e.getMessage());
            }
        } catch (Exception ex) {
            LOGGER.warn("Error has happened while polling storage account: {}", ex.getMessage());
        }
        return storageCheckerContext.getExpectedStatus() == status;
    }

    public enum StorageStatus {
        SUCCEEDED("Succeeded"), NOTFOUND("NotFound"), OTHER("Other");
        private final String value;

        StorageStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}
