package com.sequenceiq.freeipa.kerberosmgmt.v1;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.directory.server.kerberos.shared.keytab.KeytabEncoder;
import org.apache.directory.server.kerberos.shared.keytab.KeytabEntry;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ActorKerberosKey;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.cloudbreak.common.service.Clock;

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
    private static final byte KEY_VERSION_NUMBER_ZERO = 0;

    @Inject
    private Clock clock;

    private KeytabEntry toKeytabEntry(String user, String realm, ActorKerberosKey actorKerberosKey) {
        requireNonNull(user, "user must not be null");
        requireNonNull(realm, "realm must not be null");
        requireNonNull(actorKerberosKey, "actorKerberosKey must not be null");

        EncryptionType encryptionType = EncryptionType.getTypeByValue(actorKerberosKey.getKeyType());
        byte[] key = Base64Util.decodeAsByteArray(actorKerberosKey.getKeyValue());
        EncryptionKey encryptionKey = new EncryptionKey(encryptionType, key);

        KerberosTime time = new KerberosTime(clock.getCurrentTimeMillis());

        return new KeytabEntry(user + '@' + realm, PrincipalNameType.KRB_NT_PRINCIPAL.getValue(), time, KEY_VERSION_NUMBER_ZERO, encryptionKey);
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
        keytab.setEntries(keytabEntries);
        KeytabEncoder encoder = new KeytabEncoder();
        ByteBuffer keyByteBuffer = encoder.write(keytab.getKeytabVersion(), keytab.getEntries());

        return Base64Util.encode(keyByteBuffer.array());
    }
}
