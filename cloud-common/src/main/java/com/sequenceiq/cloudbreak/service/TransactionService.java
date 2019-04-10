package com.sequenceiq.cloudbreak.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionService.class);

    private static final long TX_DURATION_OK = 1000L;

    private static final long TX_DURATION_ERROR = 3000L;

    @Inject
    private TransactionExecutorService transactionExecutorService;

    @Inject
    private Clock clock;

    @Value("${cb.log.transaction.stacktrace:true}")
    private boolean logTransactionStacktrace;

    public <T> T required(Supplier<T> callback) throws TransactionExecutionException {
        long start = clock.getCurrentTimeMillis();
        try {
            return transactionExecutorService.required(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction failed", e);
        } finally {
            processTransactionDuration(start);
        }
    }

    public <T> T requiresNew(Supplier<T> callback) throws TransactionExecutionException {
        long start = clock.getCurrentTimeMillis();
        try {
            return transactionExecutorService.requiresNew(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction failed", e);
        } finally {
            processTransactionDuration(start);
        }
    }

    public <T> T mandatory(Supplier<T> callback) throws TransactionExecutionException {
        long start = clock.getCurrentTimeMillis();
        try {
            return transactionExecutorService.mandatory(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction failed", e);
        } finally {
            processTransactionDuration(start);
        }
    }

    public <T> T supports(Supplier<T> callback) throws TransactionExecutionException {
        long start = clock.getCurrentTimeMillis();
        try {
            return transactionExecutorService.supports(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction failed", e);
        } finally {
            processTransactionDuration(start);
        }
    }

    public <T> T notSupported(Supplier<T> callback) throws TransactionExecutionException {
        long start = clock.getCurrentTimeMillis();
        try {
            return transactionExecutorService.notSupported(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction failed", e);
        } finally {
            processTransactionDuration(start);
        }
    }

    public <T> T never(Supplier<T> callback) throws TransactionExecutionException {
        long start = clock.getCurrentTimeMillis();
        try {
            return transactionExecutorService.never(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction failed", e);
        } finally {
            processTransactionDuration(start);
        }
    }

    private void processTransactionDuration(long start) {
        long duration = clock.getCurrentTimeMillis() - start;
        if (TX_DURATION_ERROR < duration) {
            if (logTransactionStacktrace) {
                LOGGER.error("Transaction duration was critical, took {}ms at: {}", duration, generateStackTrace());
            } else {
                LOGGER.error("Transaction duration was critical, took {}ms", duration);
            }
        } else if (TX_DURATION_OK < duration) {
            if (logTransactionStacktrace) {
                LOGGER.info("Transaction duration was high, took {}ms at: {}", duration, generateStackTrace());
            } else {
                LOGGER.info("Transaction duration was high, took {}ms", duration);
            }
        }
    }

    private String generateStackTrace() {
        return String.join("\n\t", Arrays.stream(Thread.currentThread().getStackTrace()).map(StackTraceElement::toString)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public static class TransactionExecutionException extends Exception {
        public TransactionExecutionException(String message, RuntimeException cause) {
            super(message, cause);
        }

        @Override
        public synchronized RuntimeException getCause() {
            return (RuntimeException) super.getCause();
        }
    }

    public static class TransactionRuntimeExecutionException extends RuntimeException {
        public TransactionRuntimeExecutionException(TransactionExecutionException cause) {
            super(cause);
        }

        public RuntimeException getOriginalCause() {
            return ((TransactionExecutionException) getCause()).getCause();
        }
    }

    @Service
    @Transactional
    private static class TransactionExecutorServiceProd implements TransactionExecutorService {

        TransactionExecutorServiceProd() {
        }

        @Override
        @Transactional(TxType.REQUIRED)
        public <T> T required(Supplier<T> callback) {
            return callback.get();
        }

        @Override
        @Transactional(TxType.REQUIRES_NEW)
        public <T> T requiresNew(Supplier<T> callback) {
            return callback.get();
        }

        @Override
        @Transactional(TxType.MANDATORY)
        public <T> T mandatory(Supplier<T> callback) {
            return callback.get();
        }

        @Override
        @Transactional(TxType.SUPPORTS)
        public <T> T supports(Supplier<T> callback) {
            return callback.get();
        }

        @Override
        @Transactional(TxType.NOT_SUPPORTED)
        public <T> T notSupported(Supplier<T> callback) {
            return callback.get();
        }

        @Override
        @Transactional(TxType.NEVER)
        public <T> T never(Supplier<T> callback) {
            return callback.get();
        }
    }
}
