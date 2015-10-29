package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;

import com.sequenceiq.cloudbreak.cloud.model.generic.CloudTypes;

/**
 * Virtual machine types of a platform
 *
 * @see CloudTypes
 * @see VmType
 */
public class VmTypes extends CloudTypes<VmType> {
    public VmTypes(Collection<VmType> types, VmType defaultType) {
        super(types, defaultType);
    }
}
