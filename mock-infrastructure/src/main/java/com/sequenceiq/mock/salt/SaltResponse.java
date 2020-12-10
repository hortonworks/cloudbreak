package com.sequenceiq.mock.salt;

import java.util.List;
import java.util.Map;

public interface SaltResponse {

    Object run(String mockUuid, Map<String, List<String>> params) throws Exception;

    String cmd();
}
