package com.sequenceiq.cloudbreak.service.stack;

import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines;

public interface CloudConnectorParameterService {
    PlatformVariants getPlatformVariants();

    PlatformDisks getDiskTypes();

    PlatformVirtualMachines getVmtypes();

    PlatformRegions getRegions();

}
