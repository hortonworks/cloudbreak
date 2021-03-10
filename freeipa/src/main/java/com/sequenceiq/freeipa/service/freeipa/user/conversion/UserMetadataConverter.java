package com.sequenceiq.freeipa.service.freeipa.user.conversion;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.service.freeipa.user.model.JsonUserMetadata;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserMetadataConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserMetadataConverter.class);

    private static final ObjectReader OBJECT_READER = new ObjectMapper().readerFor(JsonUserMetadata.class)
            .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static final ObjectWriter OBJECT_WRITER = new ObjectMapper().writerFor(JsonUserMetadata.class);

    public Optional<UserMetadata> toUserMetadata(User user) {
        requireNonNull(user);

        if (StringUtils.isBlank(user.getTitle())) {
            // No need to warn, because this can happen under normal circumstances, if the FreeIPA user record was created before the credential
            // update optimization entitlement was granted to the account.
            return Optional.empty();
        }

        Optional<UserMetadata> userMetadata = Optional.empty();
        Optional<JsonUserMetadata> jsonUserMetadata = getJsonUserMetadata(user);

        if (jsonUserMetadata.isPresent() && isValidActorCrn(jsonUserMetadata.get().getCrn(), user.getUid())) {
            Optional<UserManagementProto.WorkloadUserSyncActorMetadata> protoMetadata = decodeProtoMetadata(jsonUserMetadata.get().getMeta(), user.getUid());
            if (protoMetadata.isPresent()) {
                userMetadata = Optional.of(new UserMetadata(jsonUserMetadata.get().getCrn(), protoMetadata.get().getWorkloadCredentialsVersion()));
            }
        }

        return userMetadata;
    }

    public String toUserMetadataJson(UserMetadata userMetadata) {
        requireNonNull(userMetadata);
        return toUserMetadataJson(userMetadata.getCrn(), userMetadata.getWorkloadCredentialsVersion());
    }

    public String toUserMetadataJson(String crn, long workloadCredentialsVersion) {
        checkArgument(!Strings.isNullOrEmpty(crn));

        UserManagementProto.WorkloadUserSyncActorMetadata protoMetadata = UserManagementProto.WorkloadUserSyncActorMetadata.newBuilder()
                .setWorkloadCredentialsVersion(workloadCredentialsVersion)
                .build();
        String protoMetadataBase64Encoded = BaseEncoding.base64().encode(protoMetadata.toByteArray());

        JsonUserMetadata jsonUserMetadata = new JsonUserMetadata(crn, protoMetadataBase64Encoded);
        try {
            return OBJECT_WRITER.writeValueAsString(jsonUserMetadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(String.format("Unable to serialize %s as JSON: ", jsonUserMetadata), e);
        }
    }

    private Optional<JsonUserMetadata> getJsonUserMetadata(User user) {
        Optional<JsonUserMetadata> jsonUserMetadata = Optional.empty();
        try {
            jsonUserMetadata = Optional.of(OBJECT_READER.readValue(user.getTitle()));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Unable to parse metadata string {} for user with workload username {}", user.getTitle(), user.getUid(), e);
        }
        return jsonUserMetadata;
    }

    private boolean isValidActorCrn(String crnToValidate, String username) {
        Optional<Crn> crn = Optional.empty();
        try {
            crn = Optional.of(Crn.safeFromString(crnToValidate));
        } catch (CrnParseException e) {
            LOGGER.warn("Unable to parse CRN {} in metadata for user with workload username {}", crnToValidate, username, e);
        }

        boolean validCrn = false;
        if (crn.isPresent()) {
            Crn crnVal = crn.get();
            if (crnVal.getService() == Crn.Service.IAM &&
                    (crnVal.getResourceType() == Crn.ResourceType.USER || crnVal.getResourceType() == Crn.ResourceType.MACHINE_USER)) {
                validCrn = true;
            } else {
                LOGGER.warn("CRN {} in metadata for user with workload username {} is not a user or machine user CRN", crnToValidate, username);
            }
        }

        return validCrn;
    }

    private Optional<UserManagementProto.WorkloadUserSyncActorMetadata> decodeProtoMetadata(String encodedMetadata, String username) {
        Optional<UserManagementProto.WorkloadUserSyncActorMetadata> protoMetadata = Optional.empty();
        try {
            byte[] base64Decoded = BaseEncoding.base64().decode(encodedMetadata);
            protoMetadata = Optional.of(UserManagementProto.WorkloadUserSyncActorMetadata.parseFrom(base64Decoded));
        } catch (Exception e) {
            LOGGER.warn("Unable to decode meta attribute {} for user work workload username {}", encodedMetadata, username, e);
        }
        return protoMetadata;
    }
}
