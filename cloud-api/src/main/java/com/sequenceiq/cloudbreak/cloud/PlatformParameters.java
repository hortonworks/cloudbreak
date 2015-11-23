package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.Regions;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;

/**
 * Platform parameters.
 *
 */
public interface PlatformParameters {

    /**
     * Parameters for script generation
     *
     * @return the {@link ScriptParams} of a platform
     */
    ScriptParams scriptParams();

    /**
     * DiskTypes of a platform
     *
     * @return the {@link DiskTypes} of a platform
     */
    DiskTypes diskTypes();

    /**
     * Regions of a platform
     *
     * @return the {@link Regions} of a platform
     */
    Regions regions();

    /**
     * Virtual machine types of a platform
     *
     * @return the {@link VmTypes} of a platform
     */
    VmTypes vmTypes();

    /**
     * Return the availability zones of a platform
     *
     * @return the {@link AvailabilityZones} of a platform
     */
    AvailabilityZones availabilityZones();
}
