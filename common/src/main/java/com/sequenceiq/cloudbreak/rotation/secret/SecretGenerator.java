package com.sequenceiq.cloudbreak.rotation.secret;

import java.util.Map;

public interface SecretGenerator {

    String generate(Map<String, Object> arguments);
}
