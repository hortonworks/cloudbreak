package com.sequenceiq.cloudbreak.apiformat;

import java.util.Optional;
import java.util.function.Function;

public interface ApiFormatRule extends Function<Class<?>, Optional<String>> {
}
