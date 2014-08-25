package com.sequenceiq.cloudbreak.controller.json;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = StatusUpdateRequest.class, name = "statusUpdate"),
        @JsonSubTypes.Type(value = RoleUpdateRequest.class, name = "roleUpdate"),
        @JsonSubTypes.Type(value = InviteConfirmationRequest.class, name = "inviteConfirmation") })

public interface UpdateRequest extends JsonEntity {

}
