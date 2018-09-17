package com.sequenceiq.it.cloudbreak.newway.mock.v2;

import java.util.Map;

import com.sequenceiq.it.spark.ITResponse;

public interface RequestMock<T> {

    ITResponse getReturn(Map<String, Object> map);

    T post();

    T delete();

}
