package com.sequenceiq.freeipa.util;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

public final class ASNEncoder {

    private ASNEncoder() {

    }

    static DERSequence makeSalt(int type, String salt) throws Exception {
        return new DERSequence(new ASN1Encodable[]{
            new DERTaggedObject(true, 0, new DERInteger(type)),
            new DERTaggedObject(true, 1, new DEROctetString(salt.getBytes("UTF-8")))
        });
    }

    static DERSequence makeEncryptionKey(int enctype, byte[] value) {
        return new DERSequence(new ASN1Encodable[]{
            new DERTaggedObject(true, 0, new DERInteger(enctype)),
            new DERTaggedObject(true, 1, new DEROctetString(value))
        });
    }

    static DERSequence makeKrbKey(DERSequence salt, DERSequence key) {
        return new DERSequence(new ASN1Encodable[] {
            new DERTaggedObject(true, 0, salt),
            new DERTaggedObject(true, 1, key)
        });
    }

}
