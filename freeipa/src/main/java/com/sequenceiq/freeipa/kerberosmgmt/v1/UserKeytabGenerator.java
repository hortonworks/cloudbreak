package com.sequenceiq.freeipa.kerberosmgmt.v1;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ActorKerberosKey;
import com.sequenceiq.cloudbreak.common.service.Clock;
import org.apache.kerby.kerberos.kerb.keytab.Keytab;
import org.apache.kerby.kerberos.kerb.keytab.KeytabEntry;
import org.apache.kerby.kerberos.kerb.type.KerberosTime;
import org.apache.kerby.kerberos.kerb.type.base.EncryptionKey;
import org.apache.kerby.kerberos.kerb.type.base.EncryptionType;
import org.apache.kerby.kerberos.kerb.type.base.PrincipalName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Kerberos keytab generator - This class is repsonsible for generating a keytab using existing keys
 * belonging to an actor in User Management Service.
 */
@Component
public class UserKeytabGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserKeytabGenerator.class);

    // For the key version number (kvno) in the keytab entries, we use a value of 0. Based on
    // expirementiation we've found that for user principles, keytab entries with kvno of zero
    // works fine (in fact, any arbitrary number should work), regardless of what the actual key
    // versions are set in the KDC / Directory Server.
    private static final int KEY_VERSION_NUMBER_ZERO = 0;

    @Inject
    private Clock clock;

    private KeytabEntry toKeytabEntry(String user, String realm, ActorKerberosKey actorKerberosKey) {
        requireNonNull(user, "user must not be null");
        requireNonNull(realm, "realm must not be null");
        requireNonNull(actorKerberosKey, "actorKerberosKey must not be null");

        PrincipalName principalName = new PrincipalName(user + '@' + realm);

        EncryptionType encryptionType = EncryptionType.fromValue(actorKerberosKey.getKeyType());
        byte[] key = Base64.getDecoder().decode(actorKerberosKey.getKeyValue());
        EncryptionKey encryptionKey = new EncryptionKey(encryptionType, key);

        KerberosTime time = new KerberosTime(clock.getCurrentTimeMillis());

        return new KeytabEntry(principalName, time, KEY_VERSION_NUMBER_ZERO, encryptionKey);
    }

    public String generateKeytabBase64(String username, String realm, List<ActorKerberosKey> actorKerberosKeys) {
        LOGGER.info("Generating keytab for username = {} with realm = {}", username, realm);

        if (actorKerberosKeys.isEmpty()) {
            throw new IllegalArgumentException("Expected at least 1 actorKerberosKeys");
        }

        List<KeytabEntry> keytabEntries = actorKerberosKeys.stream()
                .map(key -> toKeytabEntry(username, realm, key))
                .collect(Collectors.toList());
        Keytab keytab = new Keytab();
        keytab.addKeytabEntries(keytabEntries);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            keytab.store(outputStream);
            byte[] keyBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(keyBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate keytab", e);
        }
    }
}
