package com.sequenceiq.cloudbreak.auth.altus.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsOperationException;

public class CrnChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrnChecker.class);

    private CrnChecker() {

    }

    public static void checkAccountIdIsNotInternal(String accountId) {
        if (StringUtils.equals(accountId, InternalCrnBuilder.INTERNAL_ACCOUNT)) {
            throw new UmsOperationException(
                    String.format("Internal error happened when preparing request for IAM service call, account ID was: %s. ", accountId));
        }
    }

    public static void warnIfAccountIdIsInternal(String accountId) {
        if (StringUtils.equals(accountId, InternalCrnBuilder.INTERNAL_ACCOUNT)) {
            LOGGER.warn("Invalid invocation happened",
                    new UmsOperationException(String.format("Not fatal for now, but request was created for IAM service with accountId: %s. ", accountId)));
        }
    }
}
