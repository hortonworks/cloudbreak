package com.sequenceiq.cloudbreak.auth.altus.model;

/**
 * Represents a user crn with their assigned resource role for a specific resource.
 */
public record UserWithResourceRole(String userCrn, String resourceRoleCrn) {

}
