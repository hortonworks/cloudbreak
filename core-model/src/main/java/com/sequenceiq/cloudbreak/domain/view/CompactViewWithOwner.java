package com.sequenceiq.cloudbreak.domain.view;

public abstract class CompactViewWithOwner extends CompactView {
    private String owner;

    public CompactViewWithOwner() {
    }

    public CompactViewWithOwner(Long id, String name, String owner) {
        super(id, name);
        this.owner = owner;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
