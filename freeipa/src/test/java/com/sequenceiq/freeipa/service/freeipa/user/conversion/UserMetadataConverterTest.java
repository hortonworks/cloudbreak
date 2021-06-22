package com.sequenceiq.freeipa.service.freeipa.user.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;

class UserMetadataConverterTest {

    private static final String VALID_USER_CRN = CrnTestUtil.getUserCrnBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setResource(UUID.randomUUID().toString())
            .build().toString();

    private static final String VALID_MACHINE_USER_CRN = CrnTestUtil.getMachineUserCrnBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setResource(UUID.randomUUID().toString())
            .build().toString();

    // This string was created by calling makeValidEncodedMeta() with an argument of VALID_ENCODED_META_VERSION.
    // We capture the encoded value as a string constant so that if the metadata proto or encoding is changed in
    // some way, this test can verify backward compatibility with the original format exemplified by this string.
    private static final String VALID_ENCODED_META = "CAo=";

    private static final long VALID_ENCODED_META_VERSION = 5L;

    private final UserMetadataConverter underTest = new UserMetadataConverter();

    @Test
    void testToUserMetadata() {
        testToUserMetadataValidInput("{\"crn\": \"" + VALID_USER_CRN + "\", \"meta\": \"" + VALID_ENCODED_META + "\"}",
                VALID_USER_CRN, VALID_ENCODED_META_VERSION);
        testToUserMetadataValidInput("{\"crn\": \"" + VALID_MACHINE_USER_CRN + "\", \"meta\": \"" + VALID_ENCODED_META + "\"}",
                VALID_MACHINE_USER_CRN, VALID_ENCODED_META_VERSION);
        testToUserMetadataValidInput("{\"crn\": \"" + VALID_USER_CRN + "\", \"meta\": \"" + makeValidEncodedMeta(0L) + "\"}",
                VALID_USER_CRN, 0L);
        testToUserMetadataValidInput("{\"crn\": \"" + VALID_MACHINE_USER_CRN + "\", \"meta\": \"" + makeValidEncodedMeta(-1L) + "\"}",
                VALID_MACHINE_USER_CRN, -1L);
    }

    @Test
    void testToUserMetadataAllowsUnknownFields() {
        testToUserMetadataValidInput("{\"crn\": \"" + VALID_USER_CRN + "\", \"meta\": \"" + VALID_ENCODED_META + "\", \"extra\": \"extra value\"}",
                VALID_USER_CRN, VALID_ENCODED_META_VERSION);
        testToUserMetadataValidInput("{\"extra\": \"extra value\", \"crn\": \"" + VALID_MACHINE_USER_CRN + "\", \"meta\": \"" + VALID_ENCODED_META + "\"}",
                VALID_MACHINE_USER_CRN, VALID_ENCODED_META_VERSION);
        testToUserMetadataValidInput("{\"crn\": \"" + VALID_USER_CRN + "\", \"meta\": \"" + makeValidEncodedMeta(0L) + "\", \"extra\": \"extra value\"}",
                VALID_USER_CRN, 0L);
    }

    @Test
    void testToUserMetadataEmptyJson() {
        User user = new User();
        user.setUid("username");
        assertFalse(underTest.toUserMetadata(user).isPresent());

        user.setTitle("");
        assertFalse(underTest.toUserMetadata(user).isPresent());

        user.setTitle("       ");
        assertFalse(underTest.toUserMetadata(user).isPresent());
    }

    @Test
    void testToUserMetadataInvalidJson() {
        ImmutableList<String> invalidJsonDocs = ImmutableList.of(
                "not-valid-json",
                "{",
                "{}",
                "{\"crn\": \"" + VALID_USER_CRN + "\"}",
                "{\"meta\": \"" + VALID_ENCODED_META + "\"}",
                "{\"crn\": \"" + VALID_USER_CRN + "\", \"extra\": \"extra value\"}",
                "{\"extra\": \"extra value\"}");

        testToUserMetadataInvalidInput(invalidJsonDocs);
    }

    @Test
    void testToUserMetadataInvalidCrn() {
        String groupCrn = CrnTestUtil.getGroupCrnBuilder()
                .setAccountId(UUID.randomUUID().toString())
                .setResource(UUID.randomUUID().toString())
                .build().toString();
        String envCrn = CrnTestUtil.getEnvironmentCrnBuilder()
                .setResource(UUID.randomUUID().toString())
                .setAccountId(UUID.randomUUID().toString())
                .build().toString();
        ImmutableList<String> validJsonDocsWithInvalidCrns = ImmutableList.of(
                "{\"crn\": \"" + "" + "\", \"meta\": \"" + VALID_ENCODED_META + "\"}",
                "{\"crn\": \"" + "foo" + "\", \"meta\": \"" + VALID_ENCODED_META + "\"}",
                "{\"crn\": \"" + "crn:foo" + "\", \"meta\": \"" + VALID_ENCODED_META + "\"}",
                "{\"crn\": \"" + groupCrn + "\", \"meta\": \"" + VALID_ENCODED_META + "\"}",
                "{\"crn\": \"" + envCrn + "\", \"meta\": \"" + VALID_ENCODED_META + "\"}");

        testToUserMetadataInvalidInput(validJsonDocsWithInvalidCrns);
    }

    @Test
    void testToUserMetadataInvalidMeta() {
        ImmutableList<String> validJsonDocsWithInvalidMeta = ImmutableList.of(
                "{\"crn\": \"" + VALID_USER_CRN + "\", \"meta\": \"" + "@!-invalid-base64-@!" + "\"}",
                "{\"crn\": \"" + VALID_USER_CRN + "\", \"meta\": \"" + BaseEncoding.base64().encode("foo".getBytes()) + "\"}");

        testToUserMetadataInvalidInput(validJsonDocsWithInvalidMeta);
    }

    @Test
    void testToUserMetaDataJson() {
        testToUserMetadataJsonValidInput(VALID_USER_CRN, 1L);
        testToUserMetadataJsonValidInput(VALID_MACHINE_USER_CRN, 0L);
        testToUserMetadataJsonValidInput(VALID_USER_CRN, -1L);
    }

    private void testToUserMetadataValidInput(String input, String expectedCrn, long expectedWorkloadCredentialsVersion) {
        User user = new User();
        user.setUid("username");
        user.setTitle(input);
        Optional<UserMetadata> decoded = underTest.toUserMetadata(user);
        assertTrue(decoded.isPresent());
        assertEquals(expectedCrn, decoded.get().getCrn());
        assertEquals(expectedWorkloadCredentialsVersion, decoded.get().getWorkloadCredentialsVersion());

        String encoded = underTest.toUserMetadataJson(decoded.get());
        assertTrue(encoded.contains(expectedCrn));
        assertTrue(encoded.contains(makeValidEncodedMeta(expectedWorkloadCredentialsVersion)));
    }

    private void testToUserMetadataInvalidInput(List<String> inputs) {
        inputs.forEach(input -> {
            User user = new User();
            user.setUid("username");
            user.setTitle(input);
            assertFalse(underTest.toUserMetadata(user).isPresent());
        });
    }

    private void testToUserMetadataJsonValidInput(String crn, long workloadCredentialsVersion) {
        UserMetadata input = new UserMetadata(crn, workloadCredentialsVersion);
        String encoded = underTest.toUserMetadataJson(input);
        assertTrue(encoded.contains(crn));
        assertTrue(encoded.contains(makeValidEncodedMeta(workloadCredentialsVersion)));

        User user = new User();
        user.setUid("username");
        user.setTitle(encoded);
        Optional<UserMetadata> decoded = underTest.toUserMetadata(user);
        assertTrue(decoded.isPresent());
        assertEquals(input, decoded.get());
    }

    private static String makeValidEncodedMeta(long workloadCredentialsVersion) {
        UserManagementProto.WorkloadUserSyncActorMetadata meta = UserManagementProto.WorkloadUserSyncActorMetadata.newBuilder()
                .setWorkloadCredentialsVersion(workloadCredentialsVersion)
                .build();
        return BaseEncoding.base64().encode(meta.toByteArray());
    }
}