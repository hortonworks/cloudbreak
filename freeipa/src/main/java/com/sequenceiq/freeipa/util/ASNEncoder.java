package com.sequenceiq.freeipa.util;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ActorKerberosKey;

import java.util.Base64;
import java.util.List;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

public final class ASNEncoder {

    private static final int TAG_0 = 0;

    private static final int TAG_1 = 1;

    private static final int TAG_2 = 2;

    private static final int TAG_3 = 3;

    private static final int TAG_4 = 4;

    private static final int NUM_KEYS = 2;

    private ASNEncoder() {

    }

    private static DERSequence makeSalt(int type, String salt) throws Exception {
        return new DERSequence(new ASN1Encodable[]{
            new DERTaggedObject(true, TAG_0, new DERInteger(type)),
            new DERTaggedObject(true, TAG_1, new DEROctetString(salt.getBytes("UTF-8")))
        });
    }

    private static DERSequence makeEncryptionKey(int enctype, byte[] value) {
        return new DERSequence(new ASN1Encodable[]{
            new DERTaggedObject(true, TAG_0, new DERInteger(enctype)),
            new DERTaggedObject(true, TAG_1, new DEROctetString(value))
        });
    }

    private static DERSequence makeKrbKey(DERSequence salt, DERSequence key) {
        return new DERSequence(new ASN1Encodable[] {
            new DERTaggedObject(true, TAG_0, salt),
            new DERTaggedObject(true, TAG_1, key)
        });
    }

    public static String getASNEncodedKrbPrincipalKey(List<ActorKerberosKey> keys) throws Exception {
        if (keys == null || keys.size() < NUM_KEYS) {
            throw new IllegalArgumentException("Invalid kerberos keys provided");
        }

        ASN1Encodable[] asn1Encodables = new ASN1Encodable[NUM_KEYS];

        for (int i = 0; i < NUM_KEYS; i++) {
            ActorKerberosKey key = keys.get(i);
            byte[] byteValue = Base64.getDecoder().decode(key.getKeyValue().getBytes("UTF-8"));
            asn1Encodables[i] = makeKrbKey(makeSalt(key.getSaltType(), key.getSaltValue()), makeEncryptionKey(key.getKeyType(), byteValue));
        }

        DERSequence krbKeys = new DERSequence(asn1Encodables);

        DERSequence krbKeySet = new DERSequence(new ASN1Encodable[]{
            new DERTaggedObject(true, TAG_0, new DERInteger(1)),
            new DERTaggedObject(true, TAG_1, new DERInteger(1)),
            new DERTaggedObject(true, TAG_2, new DERInteger(1)),
            new DERTaggedObject(true, TAG_3, new DERInteger(1)),
            new DERTaggedObject(true, TAG_4, krbKeys)
        });
        return Base64.getEncoder().encodeToString(krbKeySet.getEncoded());
    }
}
