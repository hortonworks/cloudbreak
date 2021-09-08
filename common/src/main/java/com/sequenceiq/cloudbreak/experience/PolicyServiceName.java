package com.sequenceiq.cloudbreak.experience;

import java.util.Set;

public enum PolicyServiceName {

    LIFTIE("Kubernetes cluster manager", Set.of("liftie")),
    DWX("Data Warehouse Experience", Set.of("dwx", "dw", "cdw")),
    MLX("Machine Learning Experience", Set.of("mlx", "ml"));

    private final String publicName;

    private final Set<String> internalAlternatives;

    PolicyServiceName(String publicName, Set<String> internalAlternatives) {
        this.publicName = publicName;
        this.internalAlternatives = internalAlternatives;
    }

    public String getPublicName() {
        return publicName;
    }

    public Set<String> getInternalAlternatives() {
        return internalAlternatives;
    }

    public boolean hasMatchForInternalAlternativesWithIgnoreCase(String policyName) {
        for (String alternative : getInternalAlternatives()) {
            if (alternative.equalsIgnoreCase(policyName)) {
                return true;
            }
        }
        return false;
    }

}
