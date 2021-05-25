package com.sequenceiq.cdp.databus.processor;

import java.util.concurrent.BlockingDeque;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cdp.databus.cache.AccountDatabusConfigCache;
import com.sequenceiq.cdp.databus.client.DatabusClient;
import com.sequenceiq.cdp.databus.model.DatabusRecordInput;
import com.sequenceiq.cdp.databus.model.DatabusRequestContext;
import com.sequenceiq.cdp.databus.model.PutRecordRequest;
import com.sequenceiq.cdp.databus.model.PutRecordResponse;
import com.sequenceiq.cdp.databus.model.exception.DatabusRecordProcessingException;
import com.sequenceiq.cdp.databus.service.AccountDatabusConfigService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;

/**
 * Worker class that uses databus client for record processing. (with put record operation) It process data in order from a blocking queue.
 * Holds an im-memory cache as well that is used to avoid frequent database operations (obtainging databus credentials by account id ).
 * @param <P> type of a databus record processor implementation.
 */
public class DatabusRecordWorker<P extends AbstractDatabusRecordProcessor> extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabusRecordWorker.class);

    private final DatabusClient.Builder clientBuilder;

    private final BlockingDeque<DatabusRecordInput> processingQueue;

    private final AccountDatabusConfigService accountDatabusConfigService;

    private final AccountDatabusConfigCache accountDatabusConfigCache;

    private final P databusRecordProcessor;

    private DatabusClient dataBusClient;

    DatabusRecordWorker(String name, DatabusClient.Builder clientBuilder, AccountDatabusConfigService accountDatabusConfigService,
            AccountDatabusConfigCache accountDatabusConfigCache, BlockingDeque<DatabusRecordInput> processingQueue, P databusRecordProcessor) {
        super(name);
        this.clientBuilder = clientBuilder;
        this.accountDatabusConfigService = accountDatabusConfigService;
        this.accountDatabusConfigCache = accountDatabusConfigCache;
        this.processingQueue = processingQueue;
        this.databusRecordProcessor = databusRecordProcessor;
    }

    public DatabusClient getClient() {
        if (dataBusClient == null) {
            dataBusClient = clientBuilder.build();
        }
        return dataBusClient;
    }

    @Override
    public void run() {
        LOGGER.info("Start processing databus records. [name:{}]", getName());
        while (true) {
            try {
                processRecordInput(processingQueue.take());
            } catch (InterruptedException ie) {
                LOGGER.debug("DataBus record processing interrupted: {}", ie.getMessage());
                if (dataBusClient != null) {
                    dataBusClient.shutdown();
                }
                break;
            }
        }
    }

    /**
     * Process databus records. Retry operation (only once) in case of 401/403 http errors.
     * In case of authentication/authorization errors it will check UMS about the machine user + access key
     * (it will re-create the machine user/access key pair if it does not exist).
     * 2xx and 3xx responses are accepted.
     * @param input databus record input.
     */
    @VisibleForTesting
    synchronized void processRecordInput(DatabusRecordInput input) {
        if (input == null || input.getDatabusRequestContext().isEmpty()) {
            LOGGER.debug("Databus input or request context cannot be empty");
            return;
        }
        DatabusRequestContext context = input.getDatabusRequestContext().get();
        buildMdcContext(context);
        String accountId = context.getAccountId();
        String machineUserName = databusRecordProcessor.getMachineUserName(accountId);
        try {
            processRecordInputWithRetry(input, accountId, machineUserName, true);
        } catch (DatabusRecordProcessingException e) {
            databusRecordProcessor.handleDatabusRecordProcessingException(input, machineUserName, accountId, e);
        } catch (Exception e) {
            databusRecordProcessor.handleUnexpectedException(input, machineUserName, accountId, e);
        }
    }

    private void processRecordInputWithRetry(DatabusRecordInput input, String accountId,
            String machineUserName, boolean clientRetry) throws DatabusRecordProcessingException {
        DataBusCredential dataBusCredential =
                accountDatabusConfigService.getOrCreateDataBusCredentials(machineUserName, accountId, accountDatabusConfigCache);
        if (dataBusCredential != null) {
            PutRecordRequest recordRequest = input.getRecordRequest().get();
            PutRecordResponse response = getClient().putRecord(recordRequest, dataBusCredential);
            int statusCode = response.getHttpCode();
            Response.Status.Family responseFamily = Response.Status.Family.familyOf(statusCode);
            boolean clientError = Response.Status.Family.CLIENT_ERROR.equals(responseFamily);
            boolean successful = isSuccess(statusCode);
            boolean serverError = Response.Status.Family.SERVER_ERROR.equals(responseFamily);
            LOGGER.debug("Response status code from databus record proceesing: {}", statusCode);
            if (successful) {
                LOGGER.debug("Successful response for databus put record. status code: {}", statusCode);
            } else if (clientError) {
                handleClientResponseError(input, accountId, machineUserName, clientRetry, statusCode);
            } else if (serverError) {
                throw new DatabusRecordProcessingException(String.format("Cannot process databus record, server error - status code: %d", statusCode));
            } else {
                throw new DatabusRecordProcessingException(String.format("Cannot process databus record, status code: %s", statusCode));
            }
        }
    }

    private void handleClientResponseError(DatabusRecordInput input, String accountId, String machineUserName, boolean clientRetry, int statusCode)
            throws DatabusRecordProcessingException {
        if (clientRetry && isAuthError(statusCode)) {
            LOGGER.debug("Client error happened. Check that machine user still exists with access key on UMS side.");
            boolean credentialStillExists = accountDatabusConfigService.checkMachineUserWithAccessKeyStillExists(
                    accountId, accountDatabusConfigCache);
            if (!credentialStillExists) {
                LOGGER.debug("Machine user does not exists anymore on UMS side.");
                try {
                    accountDatabusConfigService.cleanupCacheAndDbForAccountIdAndName(accountId, machineUserName, accountDatabusConfigCache);
                } catch (TransactionService.TransactionExecutionException e) {
                    throw new DatabusRecordProcessingException(e);
                }
            }
            processRecordInputWithRetry(input, accountId, machineUserName, false);
        } else {
            throw new DatabusRecordProcessingException(String.format("Cannot process databus record, client error - status code: %d", statusCode));
        }
    }

    private boolean isSuccess(int statusCode) {
        Response.Status.Family family = Response.Status.Family.familyOf(statusCode);
        return family == Response.Status.Family.SUCCESSFUL
                || family == Response.Status.Family.REDIRECTION;
    }

    private boolean isAuthError(int statusCode) {
        return statusCode == Response.Status.UNAUTHORIZED.getStatusCode() || statusCode == Response.Status.FORBIDDEN.getStatusCode();
    }

    private void buildMdcContext(DatabusRequestContext context) {
        MDCBuilder.buildMdc(MdcContext.builder()
                .tenant(context.getAccountId())
                .environmentCrn(context.getEnvironmentCrn())
                .resourceCrn(context.getResourceCrn())
                .resourceName(context.getResourceName()).buildMdc());
    }
}
