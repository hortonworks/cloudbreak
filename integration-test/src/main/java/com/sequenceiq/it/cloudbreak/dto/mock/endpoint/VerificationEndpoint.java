package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public interface VerificationEndpoint<T extends CloudbreakTestDto> {

    default <R> DefaultResponseConfigure<T, R> post() {
        throw new TestFailException("This endpoint does not support the POST method");
    }

    default <R> DefaultResponseConfigure<T, R> get() {
        throw new TestFailException("This endpoint does not support the GET method");
    }

    default <R> DefaultResponseConfigure<T, R> head() {
        throw new TestFailException("This endpoint does not support the HEAD method");
    }

    default <R> DefaultResponseConfigure<T, R> put() {
        throw new TestFailException("This endpoint does not support the PUT method");
    }

    default <R> DefaultResponseConfigure<T, R> delete() {
        throw new TestFailException("This endpoint does not support the DELETE method");
    }

}
