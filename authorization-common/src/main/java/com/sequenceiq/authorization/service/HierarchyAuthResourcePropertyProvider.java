package com.sequenceiq.authorization.service;

/**
 * Composite authorization framework interface to simplify usage of authorization interfaces (including hierarchical permission check),
 * since usually one component is implementing logic to get <br>
 * - resource CRN by resource name (for permission check) <br>
 * - resource CRN list by resource name list (for permission check) <br>
 * - resource name list by resource CRN list (for better error messages) <br>
 * - environment CRN by resource CRN (for hierarchical permission check) <br>
 * - environment CRN list by resource CRN list (for hierarchical permission check)
 */
public interface HierarchyAuthResourcePropertyProvider extends CompositeAuthResourcePropertyProvider, AuthorizationEnvironmentCrnProvider,
        AuthorizationEnvironmentCrnListProvider {
}
