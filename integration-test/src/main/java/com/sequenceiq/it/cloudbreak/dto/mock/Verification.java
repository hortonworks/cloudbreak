package com.sequenceiq.it.cloudbreak.dto.mock;

import java.util.List;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.verification.Call;

public interface Verification<T> {
    void handle(String path, Method method, CloudbreakClient client, List<Call> calls);
}
