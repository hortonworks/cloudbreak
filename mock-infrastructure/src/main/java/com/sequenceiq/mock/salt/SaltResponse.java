package com.sequenceiq.mock.salt;

public interface SaltResponse {

    Object run(String mockUuid, String body) throws Exception;

    String cmd();
}
