package com.sequenceiq.freeipa.client.operation;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;

public class BatchOperation<T> extends AbstractFreeipaOperation<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchOperation.class);

    private final List<Object> operations;

    private final BiConsumer<String, String> warnings;

    private final Set<FreeIpaErrorCodes> acceptableErrorCodes;

    private BatchOperation(List<Object> operations, BiConsumer<String, String> warnings, Set<FreeIpaErrorCodes> acceptableErrorCodes) {
        this.operations = operations;
        this.warnings = warnings;
        this.acceptableErrorCodes = acceptableErrorCodes;
    }

    public static <T> BatchOperation<T> create(List<Object> operations, BiConsumer<String, String> warnings, Set<FreeIpaErrorCodes> acceptableErrorCodes) {
        return new BatchOperation<>(operations, warnings, acceptableErrorCodes);
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
    public Optional<T> invoke(FreeIpaClient freeipaClient) throws FreeIpaClientException {
        try {
            if (!operations.isEmpty()) {
                super.rpcInvoke(freeipaClient, Object.class);
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

    // This batch call works only if all the batch operation is the same, so it returns with the same type
    @Override
    public RPCResponse<T> rpcInvoke(FreeIpaClient freeipaClient, Type resultType) throws FreeIpaClientException {
        try {
            if (!operations.isEmpty()) {
                return super.rpcInvoke(freeipaClient, resultType);
            } else {
                return null;
            }
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isExceptionWithErrorCode(e, acceptableErrorCodes)) {
                LOGGER.debug(String.format("Batch call had error with acceptable error code: %s", e.getMessage()));
            } else {
                LOGGER.warn(e.getMessage());
                warnings.accept("batch call failed: ", e.getMessage());
                freeipaClient.checkIfClientStillUsable(e);
            }
            return null;
        }
    }
}
