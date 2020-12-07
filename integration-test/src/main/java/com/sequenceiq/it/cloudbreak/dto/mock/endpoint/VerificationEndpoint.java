package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public interface VerificationEndpoint<T extends CloudbreakTestDto> {

    default DefaultResponseConfigure<T> post() {
        throw new TestFailException("This endpoint does not support the POST method");
    }

    default DefaultResponseConfigure<T> get() {
        throw new TestFailException("This endpoint does not support the GET method");
    }

    default DefaultResponseConfigure<T> head() {
        throw new TestFailException("This endpoint does not support the HEAD method");
    }

    default DefaultResponseConfigure<T> put() {
        throw new TestFailException("This endpoint does not support the PUT method");
    }

    default DefaultResponseConfigure<T> delete() {
        throw new TestFailException("This endpoint does not support the DELETE method");
    }

}
