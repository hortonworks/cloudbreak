package com.sequenceiq.mock.legacy.salt;

public interface SaltResponse {

    Object run(String body) throws Exception;

    String cmd();
}
