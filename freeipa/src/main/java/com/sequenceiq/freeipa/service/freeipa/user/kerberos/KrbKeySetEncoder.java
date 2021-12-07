package com.sequenceiq.freeipa.service.freeipa.user.kerberos;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ActorKerberosKey;

/**
 * Kerberos key set encoder - This class is responsible for encoding kerberos key sets using ASN.1 encoding based on UTF-8 character set.
 *
 * Below is the schema used:
 * <code>
 *   KrbKeySet ::= SEQUENCE {
 *     attribute-major-vno       [0] INTEGER,
 *     attribute-minor-vno       [1] INTEGER,
 *     kvno                      [2] INTEGER,
 *     mkvno                     [3] INTEGER OPTIONAL,
 *     keys                      [4] SEQUENCE OF KrbKey,
 *     ...
 *   }
 *
 *   KrbKey ::= SEQUENCE {
 *     salt      [0] KrbSalt OPTIONAL,
 *     key       [1] EncryptionKey,
 *     s2kparams [2] OCTET STRING OPTIONAL,
 *     ...
 *   }
 *
 *   KrbSalt ::= SEQUENCE {
 *     type      [0] INTEGER,
 *     salt      [1] OCTET STRING OPTIONAL
 *   }
 *
 *   EncryptionKey ::= SEQUENCE {
 *     enctype   [0] INTEGER,
 *     keyvalue  [1] OCTET STRING
 *   }
 * </code>
 *
 * Details on ASN.1 Encoding can be found here: https://en.wikipedia.org/wiki/Abstract_Syntax_Notation_One
 *
 */

public final class KrbKeySetEncoder {

    private static final int TAG_ATTRIBUTE_MAJOR_VNO = 0;

    private static final int TAG_ATTRIBUTE_MINOR_VNO = 1;

    private static final int TAG_KVNO = 2;

    private static final int TAG_MKVNO = 3;

    private static final int TAG_KEYS = 4;

    private KrbKeySetEncoder() {

    }

    private static DERSequence makeSalt(int type, String salt) throws UnsupportedEncodingException {
        return new DERSequence(new ASN1Encodable[]{
            new DERTaggedObject(true, TAG_ATTRIBUTE_MAJOR_VNO, new ASN1Integer(type)),
            new DERTaggedObject(true, TAG_ATTRIBUTE_MINOR_VNO, new DEROctetString(salt.getBytes(StandardCharsets.UTF_8)))
        });
    }

    private static DERSequence makeEncryptionKey(int enctype, byte[] value) {
        return new DERSequence(new ASN1Encodable[]{
            new DERTaggedObject(true, TAG_ATTRIBUTE_MAJOR_VNO, new ASN1Integer(enctype)),
            new DERTaggedObject(true, TAG_ATTRIBUTE_MINOR_VNO, new DEROctetString(value))
        });
    }

    private static DERSequence makeKrbKey(DERSequence salt, DERSequence key) {
        return new DERSequence(new ASN1Encodable[] {
            new DERTaggedObject(true, TAG_ATTRIBUTE_MAJOR_VNO, salt),
            new DERTaggedObject(true, TAG_ATTRIBUTE_MINOR_VNO, key)
        });
    }

    public static String getASNEncodedKrbPrincipalKey(List<ActorKerberosKey> keys) throws IOException {
        ASN1Encodable[] asn1Encodables = new ASN1Encodable[keys.size()];

        for (int i = 0; i < keys.size(); i++) {
            ActorKerberosKey key = keys.get(i);
            byte[] byteValue = Base64.getDecoder().decode(key.getKeyValue().getBytes(StandardCharsets.UTF_8));
            asn1Encodables[i] = makeKrbKey(makeSalt(key.getSaltType(), key.getSaltValue()), makeEncryptionKey(key.getKeyType(), byteValue));
        }

        DERSequence krbKeys = new DERSequence(asn1Encodables);

        DERSequence krbKeySet = new DERSequence(new ASN1Encodable[]{
            // attribute-major-vno
            new DERTaggedObject(true, TAG_ATTRIBUTE_MAJOR_VNO, new ASN1Integer(1)),

            // attribute-minor-vno
            new DERTaggedObject(true, TAG_ATTRIBUTE_MINOR_VNO, new ASN1Integer(1)),

            // kvno
            new DERTaggedObject(true, TAG_KVNO, new ASN1Integer(1)),

            // mkvno
            new DERTaggedObject(true, TAG_MKVNO, new ASN1Integer(1)),

            new DERTaggedObject(true, TAG_KEYS, krbKeys)
        });
        return Base64.getEncoder().encodeToString(krbKeySet.getEncoded());
    }
}
