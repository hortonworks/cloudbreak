package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import java.util.List;
import java.util.Map;

import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;

public interface InstanceWaitObject extends WaitObject {

    List<String> getInstanceIds();

    Map<String, String> getFetchedInstanceStatuses();
}
