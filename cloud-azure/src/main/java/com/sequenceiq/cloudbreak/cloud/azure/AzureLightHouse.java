package com.sequenceiq.cloudbreak.cloud.azure;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureInteractiveLoginStatusCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.lighthouse.LightHouseMasterAppClient;
import com.sequenceiq.cloudbreak.cloud.azure.task.AzurePollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

/**
 * Created by perdos on 9/22/16.
 */
@Service
@Scope("singleton")
public class AzureLightHouse {

    public static final String XPLAT_CLI_CLIENT_ID = "04b07795-8ddb-461a-bbee-02f9e1bf7b46";

    public static final String MANAGEMENT_CORE_WINDOWS = "https://management.core.windows.net/";

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureLightHouse.class);

    private Executor executor;

    private AzureInteractiveLoginStatusCheckerContext azureInteractiveLoginStatusCheckerContext;

    @Inject
    private AzurePollTaskFactory azurePollTaskFactory;

    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    @Inject
    private LightHouseMasterAppClient lightHouseMasterAppClient;

    @PostConstruct
    public void init() {
        executor = Executors.newSingleThreadExecutor();
    }

    public Map<String, String> execute(CloudContext cloudContext, ExtendedCloudCredential extendedCloudCredential,
            CredentialNotifier credentialNotifier) {
        lightHouseMasterAppClient.createLightHouseCutomerAppInClouderaTenant();
        return new HashMap<>();
    }
}
