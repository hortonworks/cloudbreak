package com.sequenceiq.cloudbreak.service;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.WrapperException;

@Service
public class TransactionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionService.class);

    private static final long TX_DURATION_OK = 1000L;

    private static final long TX_DURATION_ERROR = 3000L;

    @Inject
    private TransactionExecutorService transactionExecutorService;

    @Inject
    private Clock clock;

    public <T> T required(TransactionCallback<T> callback) throws TransactionExecutionException {
        long start = clock.getCurrentTime();
        try {
            return transactionExecutorService.required(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        } finally {
            processTransactionDuration(start);
        }
    }

    public <T> T requiresNew(TransactionCallback<T> callback) throws TransactionExecutionException {
        long start = clock.getCurrentTime();
        try {
            return transactionExecutorService.requiresNew(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        } finally {
            processTransactionDuration(start);
        }
    }

    public <T> T mandatory(TransactionCallback<T> callback) throws TransactionExecutionException {
        long start = clock.getCurrentTime();
        try {
            return transactionExecutorService.mandatory(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        } finally {
            processTransactionDuration(start);
        }
    }

    public <T> T supports(TransactionCallback<T> callback) throws TransactionExecutionException {
        long start = clock.getCurrentTime();
        try {
            return transactionExecutorService.supports(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        } finally {
            processTransactionDuration(start);
        }
    }

    public <T> T notSupported(TransactionCallback<T> callback) throws TransactionExecutionException {
        long start = clock.getCurrentTime();
        try {
            return transactionExecutorService.notSupported(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        } finally {
            processTransactionDuration(start);
        }
    }

    public <T> T never(TransactionCallback<T> callback) throws TransactionExecutionException {
        long start = clock.getCurrentTime();
        try {
            return transactionExecutorService.never(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        } finally {
            processTransactionDuration(start);
        }
    }

    private void processTransactionDuration(long start) {
        long duration = clock.getCurrentTime() - start;
        if (TX_DURATION_ERROR < duration) {
            LOGGER.error("Transaction duration was critical, took {}ms  at: {}", duration,
                    String.join("\n\t", Arrays.stream(Thread.currentThread().getStackTrace()).map(StackTraceElement::toString).collect(Collectors.toSet())));
        } else if (TX_DURATION_OK < duration) {
            LOGGER.warn("Transaction duration was high, took {}ms at: {}", duration,
                    String.join("\n\t", Arrays.stream(Thread.currentThread().getStackTrace()).map(StackTraceElement::toString).collect(Collectors.toSet())));
        }
    }

    @FunctionalInterface
    public interface TransactionCallback<T> {
        T get() throws TransactionExecutionException;
    }

    public static class TransactionExecutionException extends Exception {
        public TransactionExecutionException(String message, Throwable cause) {
            super(message, cause);
        }

        public TransactionExecutionException(Throwable cause) {
            this(cause.getMessage(), cause);
        }
    }

    public static class TransactionRuntimeExecutionException extends RuntimeException implements WrapperException {
        public TransactionRuntimeExecutionException(TransactionExecutionException cause) {
            super(cause);
        }

        public Throwable getRootCause() {
            return getCause().getCause();
        }
    }

    @Service
    @Transactional
    private static class TransactionExecutorService {

        TransactionExecutorService() {
        }

        @Transactional(TxType.REQUIRED)
        public <T> T required(TransactionCallback<T> callback) throws TransactionExecutionException {
            return callback.get();
        }

        @Transactional(TxType.REQUIRES_NEW)
        public <T> T requiresNew(TransactionCallback<T> callback) throws TransactionExecutionException {
            return callback.get();
        }

        @Transactional(TxType.MANDATORY)
        public <T> T mandatory(TransactionCallback<T> callback) throws TransactionExecutionException {
            return callback.get();
        }

        @Transactional(TxType.SUPPORTS)
        public <T> T supports(TransactionCallback<T> callback) throws TransactionExecutionException {
            return callback.get();
        }

        @Transactional(TxType.NOT_SUPPORTED)
        public <T> T notSupported(TransactionCallback<T> callback) throws TransactionExecutionException {
            return callback.get();
        }

        @Transactional(TxType.NEVER)
        public <T> T never(TransactionCallback<T> callback) throws TransactionExecutionException {
            return callback.get();
        }
    }
}
