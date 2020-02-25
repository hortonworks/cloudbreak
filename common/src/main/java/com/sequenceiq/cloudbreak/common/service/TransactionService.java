package com.sequenceiq.cloudbreak.common.service;

import java.util.function.Supplier;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    @Autowired
    private TransactionExecutorService transactionExecutorService;

    @Autowired
    private TransactionMetricsService transactionMetricsService;

    public <T> T required(Supplier<T> callback) throws TransactionExecutionException {
        TransactionMetricsContext context = transactionMetricsService.createTransactionMetricsContext();
        try {
            return transactionExecutorService.required(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction failed", e);
        } finally {
            transactionMetricsService.processTransactionDuration(context);
        }
    }

    public void required(Runnable callback) throws TransactionExecutionException {
        TransactionMetricsContext context = transactionMetricsService.createTransactionMetricsContext();
        try {
            transactionExecutorService.required(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction failed", e);
        } finally {
            transactionMetricsService.processTransactionDuration(context);
        }
    }

    public <T> T requiresNew(Supplier<T> callback) throws TransactionExecutionException {
        TransactionMetricsContext context = transactionMetricsService.createTransactionMetricsContext();
        try {
            return transactionExecutorService.requiresNew(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction failed", e);
        } finally {
            transactionMetricsService.processTransactionDuration(context);
        }
    }

    public <T> T mandatory(Supplier<T> callback) throws TransactionExecutionException {
        TransactionMetricsContext context = transactionMetricsService.createTransactionMetricsContext();
        try {
            return transactionExecutorService.mandatory(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction failed", e);
        } finally {
            transactionMetricsService.processTransactionDuration(context);
        }
    }

    public <T> T supports(Supplier<T> callback) throws TransactionExecutionException {
        TransactionMetricsContext context = transactionMetricsService.createTransactionMetricsContext();
        try {
            return transactionExecutorService.supports(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction failed", e);
        } finally {
            transactionMetricsService.processTransactionDuration(context);
        }
    }

    public <T> T notSupported(Supplier<T> callback) throws TransactionExecutionException {
        TransactionMetricsContext context = transactionMetricsService.createTransactionMetricsContext();
        try {
            return transactionExecutorService.notSupported(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction failed", e);
        } finally {
            transactionMetricsService.processTransactionDuration(context);
        }
    }

    public <T> T never(Supplier<T> callback) throws TransactionExecutionException {
        TransactionMetricsContext context = transactionMetricsService.createTransactionMetricsContext();
        try {
            return transactionExecutorService.never(callback);
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction failed", e);
        } finally {
            transactionMetricsService.processTransactionDuration(context);
        }
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

        @Override
        @Transactional(TxType.REQUIRED)
        public void required(Runnable callback) {
            callback.run();
        }

        @Override
        @Transactional(TxType.REQUIRES_NEW)
        public void requiresNew(Runnable callback) {
            callback.run();
        }

        @Override
        @Transactional(TxType.MANDATORY)
        public void mandatory(Runnable callback) {
            callback.run();
        }

        @Override
        @Transactional(TxType.SUPPORTS)
        public void supports(Runnable callback) {
            callback.run();
        }

        @Override
        @Transactional(TxType.NOT_SUPPORTED)
        public void notSupported(Runnable callback) {
            callback.run();
        }

        @Override
        @Transactional(TxType.NEVER)
        public void never(Runnable callback) {
            callback.run();
        }
    }
}
