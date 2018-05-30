package com.sequenceiq.cloudbreak.service;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    @Inject
    private TransactionExecutorService transactionExecutorService;

    public <T> T required(TransactionCallback<T> callback) throws TransactionExecutionException {
        try {
            return transactionExecutorService.required(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        }
    }

    public <T> T requiresNew(TransactionCallback<T> callback) throws TransactionExecutionException {
        try {
            return transactionExecutorService.requiresNew(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        }
    }

    public <T> T mandatory(TransactionCallback<T> callback) throws TransactionExecutionException {
        try {
            return transactionExecutorService.mandatory(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        }
    }

    public <T> T supports(TransactionCallback<T> callback) throws TransactionExecutionException {
        try {
            return transactionExecutorService.supports(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        }
    }

    public <T> T notSupported(TransactionCallback<T> callback) throws TransactionExecutionException {
        try {
            return transactionExecutorService.notSupported(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        }
    }

    public <T> T never(TransactionCallback<T> callback) throws TransactionExecutionException {
        try {
            return transactionExecutorService.never(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
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
    }

    public static class TransactionRuntimeExecutionException extends RuntimeException {
        public TransactionRuntimeExecutionException(TransactionExecutionException cause) {
            super(cause);
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
