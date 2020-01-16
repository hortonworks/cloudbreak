package com.sequenceiq.it.cloudbreak.dto.mock;

import java.util.List;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.TypedRequestData;
import com.sequenceiq.it.cloudbreak.mock.DefaultModel;

public interface Verification<T> {
    void handle(String path, Method method, CloudbreakClient client, DefaultModel model, List<TypedRequestData<T>> calls);
}
