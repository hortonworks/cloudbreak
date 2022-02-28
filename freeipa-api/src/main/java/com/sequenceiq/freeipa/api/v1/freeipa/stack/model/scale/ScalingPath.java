package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import java.util.Objects;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.FormFactor;

public class ScalingPath {

    private final FormFactor originalFormFactor;

    private final FormFactor targetFormFactor;

    public ScalingPath(FormFactor originalFormFactor, FormFactor targetFormFactor) {
        this.originalFormFactor = originalFormFactor;
        this.targetFormFactor = targetFormFactor;
    }

    public FormFactor getOriginalFormFactor() {
        return originalFormFactor;
    }

    public FormFactor getTargetFormFactor() {
        return targetFormFactor;
    }

    @Override
    public String toString() {
        return "ScalingPath{" +
                "originalFormFactor=" + originalFormFactor +
                ", targetFormFactor=" + targetFormFactor +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScalingPath that = (ScalingPath) o;
        return originalFormFactor == that.originalFormFactor && targetFormFactor == that.targetFormFactor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalFormFactor, targetFormFactor);
    }
}
