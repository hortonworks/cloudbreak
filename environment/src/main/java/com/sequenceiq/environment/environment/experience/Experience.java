package com.sequenceiq.environment.environment.experience;

import com.sequenceiq.environment.environment.domain.Environment;
import org.springframework.stereotype.Component;

@Component
public interface Experience {

    boolean hasExistingClusterForEnvironment(Environment environment);

}
