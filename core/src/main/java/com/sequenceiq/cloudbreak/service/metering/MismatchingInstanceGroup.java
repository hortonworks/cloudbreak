package com.sequenceiq.cloudbreak.service.metering;

import java.util.Map;

record MismatchingInstanceGroup(String instanceGroup, String instanceTypeFromTemplate, Map<String, String> instanceTypesByInstanceId) {
}
