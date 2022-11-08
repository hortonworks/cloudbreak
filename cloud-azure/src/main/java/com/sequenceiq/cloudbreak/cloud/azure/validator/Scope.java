package com.sequenceiq.cloudbreak.cloud.azure.validator;

class Scope {

    public static final String MANAGEMENT_GROUP_SCOPE = "/providers/Microsoft.Management/managementGroups/";

    private final ScopeType scopeType;

    private final String scope;

    private enum ScopeType {
        RESOURCE,
        MANAGEMENT_GROUP
    }

    private Scope(ScopeType scopeType, String scope) {
        this.scopeType = scopeType;
        this.scope = scope;
    }

    public static Scope resource(String resource) {
        return new Scope(ScopeType.RESOURCE, resource);
    }

    public static Scope managementGroup() {
        return new Scope(ScopeType.MANAGEMENT_GROUP, MANAGEMENT_GROUP_SCOPE);
    }

    public boolean match(String roleAssignmentScope) {
        switch (this.scopeType) {
            case RESOURCE:
                return roleAssignmentScope.endsWith(this.scope);
            case MANAGEMENT_GROUP:
                return roleAssignmentScope.startsWith(this.scope);
            default:
                throw new IllegalStateException("Unexpected value: " + this.scopeType);
        }
    }

    @Override
    public String toString() {
        return scope;
    }
}
