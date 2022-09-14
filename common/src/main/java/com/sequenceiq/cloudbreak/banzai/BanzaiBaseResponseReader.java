package com.sequenceiq.cloudbreak.banzai;

import java.util.Optional;

import javax.ws.rs.core.Response;

public interface BanzaiBaseResponseReader {

    <T> Optional<T> read(String target, Response response, Class<T> expectedType);

}
