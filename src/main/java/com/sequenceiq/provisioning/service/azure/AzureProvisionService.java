package com.sequenceiq.provisioning.service.azure;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.json.AzureCloudInstanceResult;
import com.sequenceiq.provisioning.controller.json.CloudInstanceResult;
import com.sequenceiq.provisioning.domain.AzureInfra;
import com.sequenceiq.provisioning.domain.CloudInstance;
import com.sequenceiq.provisioning.domain.CloudInstanceDescription;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.service.ProvisionService;

@Service
public class AzureProvisionService implements ProvisionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureProvisionService.class);

    private static final String OK_STATUS = "ok";

    private static final String DATADIR = "userdatas";

    @Override
    public CloudInstanceResult createCloudInstance(User user, CloudInstance cloudInstance) {
        String filePath = getUserJksFileName(user.emailAsFolder());
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Runnable worker = new AzureClusterCreator(user, cloudInstance, (AzureInfra) cloudInstance.getInfra(), new File(filePath));
        executor.execute(worker);
        executor.shutdown();
        return new AzureCloudInstanceResult(OK_STATUS);
    }

    private String getUserJksFileName(String user) {
        return String.format("%s/%s/%s.jks", DATADIR, user, user);
    }

    @Override
    public CloudInstanceDescription describeCloudInstance(User user, CloudInstance cloudInstance) {
        // TODO
        return null;
    }

    @Override
    public CloudInstanceDescription describeCloudInstanceWithResources(User user, CloudInstance cloudInstance) {
        // TODO
        return null;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
