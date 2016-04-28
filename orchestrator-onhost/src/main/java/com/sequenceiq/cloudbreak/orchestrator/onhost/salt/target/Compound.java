package com.sequenceiq.cloudbreak.orchestrator.onhost.salt.target;

import com.suse.salt.netapi.datatypes.target.Target;

public class Compound implements Target<String> {


    private final String compound;

    /**
     * Constructor expecting a compound pattern as string.
     *
     * @param compound compound pattern
     */
    public Compound(String compound) {
        this.compound = compound;
    }

    @Override
    public String getTarget() {
        return compound;
    }

    @Override
    public String getType() {
        return "compound";
    }
}