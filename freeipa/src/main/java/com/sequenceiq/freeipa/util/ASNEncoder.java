package com.sequenceiq.freeipa.util;

import java.util.Base64;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

public final class ASNEncoder {

    private static final int SALT_TYPE = 4;

    private static final int ENC_TYPE_17 = 17;

    private static final int ENC_TYPE_18 = 18;

    private static final int BYTE_SIZE_16 = 16;

    private static final int BYTE_SIZE_32 = 32;

    private static final int TAG_0 = 0;

    private static final int TAG_1 = 1;

    private static final int TAG_2 = 2;

    private static final int TAG_3 = 3;

    private static final int TAG_4 = 4;

    private ASNEncoder() {

    }

    static DERSequence makeSalt(int type, String salt) throws Exception {
        return new DERSequence(new ASN1Encodable[]{
            new DERTaggedObject(true, TAG_0, new DERInteger(type)),
            new DERTaggedObject(true, TAG_1, new DEROctetString(salt.getBytes("UTF-8")))
        });
    }

    static DERSequence makeEncryptionKey(int enctype, byte[] value) {
        return new DERSequence(new ASN1Encodable[]{
            new DERTaggedObject(true, TAG_0, new DERInteger(enctype)),
            new DERTaggedObject(true, TAG_1, new DEROctetString(value))
        });
    }

    static DERSequence makeKrbKey(DERSequence salt, DERSequence key) {
        return new DERSequence(new ASN1Encodable[] {
            new DERTaggedObject(true, TAG_0, salt),
            new DERTaggedObject(true, TAG_1, key)
        });
    }

    static String getASNEncodedKrbPrincipalKey(String salt) throws Exception {
        DERSequence krbKeys = new DERSequence(new ASN1Encodable[]{
            makeKrbKey(makeSalt(SALT_TYPE, salt), makeEncryptionKey(ENC_TYPE_17, new byte[BYTE_SIZE_16])),
            makeKrbKey(makeSalt(SALT_TYPE, salt), makeEncryptionKey(ENC_TYPE_18, new byte[BYTE_SIZE_32]))
        });

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
