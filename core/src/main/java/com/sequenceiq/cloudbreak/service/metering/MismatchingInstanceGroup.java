package com.sequenceiq.cloudbreak.service.metering;

import java.util.Map;

record MismatchingInstanceGroup(String instanceGroup, String originalInstanceType, Map<String, String> mismatchingInstanceTypes) {
}
