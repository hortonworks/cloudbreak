package com.sequenceiq.authorization.service;

/**
 * Composite authorization framework interface to simplify usage of authorization interfaces,
 * since usually one component is implementing logic to get <br>
 * - resource CRN by resource name (for permission check) <br>
 * - resource CRN list by resource name list (for permission check) <br>
 * - resource name list by resource CRN list (for better error messages) <br>
 */
public interface CompositeAuthResourcePropertyProvider extends AuthorizationResourceCrnProvider, AuthorizationResourceCrnListProvider,
    AuthorizationResourceNamesProvider {
}
