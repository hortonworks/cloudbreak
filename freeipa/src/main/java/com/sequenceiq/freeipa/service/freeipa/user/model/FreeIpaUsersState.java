package com.sequenceiq.freeipa.service.freeipa.user.model;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

public class FreeIpaUsersState {

    private final UsersState usersState;

    private final ImmutableMap<String, UserMetadata> userMetadataMap;

    private FreeIpaUsersState(UsersState usersState, Map<String, UserMetadata> userMetadataMap) {
        this.usersState = requireNonNull(usersState, "UsersState is null");
        this.userMetadataMap = ImmutableMap.copyOf(requireNonNull(userMetadataMap, "user metadata map is null"));
    }

    public UsersState getUsersState() {
        return usersState;
    }

    public ImmutableMap<String, UserMetadata> getUserMetadataMap() {
        return userMetadataMap;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private UsersState usersState;

        private Map<String, UserMetadata> userMetadataMap = new HashMap<>();

        public Builder setUsersState(UsersState usersState) {
            this.usersState = usersState;
            return this;
        }

        public Builder addUserMetadata(String userName, UserMetadata userMetadata) {
            userMetadataMap.put(userName, userMetadata);
            return this;
        }

        public FreeIpaUsersState build() {
            return new FreeIpaUsersState(usersState, userMetadataMap);
        }
    }

}
