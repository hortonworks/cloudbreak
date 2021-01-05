package com.sequenceiq.freeipa.client.operation;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;

public class BatchOperation extends AbstractFreeipaOperation<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchOperation.class);

    private List<Object> operations;

    private BiConsumer<String, String> warnings;

    private Set<FreeIpaErrorCodes> acceptableErrorCodes;

    private BatchOperation(List<Object> operations, BiConsumer<String, String> warnings, Set<FreeIpaErrorCodes> acceptableErrorCodes) {
        this.operations = operations;
        this.warnings = warnings;
        this.acceptableErrorCodes = acceptableErrorCodes;
    }

    public static BatchOperation create(List<Object> operations, BiConsumer<String, String> warnings, Set<FreeIpaErrorCodes> acceptableErrorCodes) {
        return new BatchOperation(operations, warnings, acceptableErrorCodes);
    }

    @Override
    public String getOperationName() {
        return "batch";
    }

    @Override
    protected List<Object> getFlags() {
        return List.of(operations);
    }

    @Override
    public Optional<Object> invoke(FreeIpaClient freeipaClient) throws FreeIpaClientException {
        try {
            if (!operations.isEmpty()) {
                rpcInvoke(freeipaClient, Object.class);
            }
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isExceptionWithErrorCode(e, acceptableErrorCodes)) {
                LOGGER.debug(String.format("Batch call had error with acceptable error code: %s", e.getMessage()));
            } else {
                LOGGER.warn(e.getMessage());
                warnings.accept("batch call failed: ", e.getMessage());
                freeipaClient.checkIfClientStillUsable(e);
            }
        }
        return Optional.empty();
    }
}
