package com.sequenceiq.cloudbreak.service;

import java.util.function.Supplier;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Service;


@Service
@Transactional
public class TransactionService {

    @Transactional(TxType.REQUIRED)
    public <T> T required(Supplier<T> callback) throws TransactionExecutionException {
        try {
            return callback.get();
        } catch (RuntimeException e) {
            throw new TransactionExecutionException("Transaction went fail", e);
        }
    }

    public static class TransactionExecutionException extends Exception {
        public TransactionExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
