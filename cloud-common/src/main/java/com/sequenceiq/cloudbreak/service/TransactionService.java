package com.sequenceiq.cloudbreak.service;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Service;

@Service
@Transactional
public class TransactionService {

    @Transactional(TxType.REQUIRED)
    public <T> T required(TransactionCallback<T> callback) throws TransactionExecutionException {
        try {
            return callback.get();
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        }
    }

    @Transactional(TxType.REQUIRES_NEW)
    public <T> T requiresNew(TransactionCallback<T> callback) throws TransactionExecutionException {
        try {
            return callback.get();
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        }
    }

    @Transactional(TxType.MANDATORY)
    public <T> T mandatory(TransactionCallback<T> callback) throws TransactionExecutionException {
        try {
            return callback.get();
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        }
    }

    @Transactional(TxType.SUPPORTS)
    public <T> T supports(TransactionCallback<T> callback) throws TransactionExecutionException {
        try {
            return callback.get();
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        }
    }

    @Transactional(TxType.NOT_SUPPORTED)
    public <T> T notSupported(TransactionCallback<T> callback) throws TransactionExecutionException {
        try {
            return callback.get();
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        }
    }

    @Transactional(TxType.NEVER)
    public <T> T never(TransactionCallback<T> callback) throws TransactionExecutionException {
        try {
            return callback.get();
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
}
