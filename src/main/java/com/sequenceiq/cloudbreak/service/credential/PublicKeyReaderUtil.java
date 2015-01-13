package com.sequenceiq.cloudbreak.service.credential;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.commons.codec.binary.Base64;

public final class PublicKeyReaderUtil {
    private static final String BEGIN_PUB_KEY = "---- BEGIN SSH2 PUBLIC KEY ----";

    private static final String END_PUB_KEY = "---- END SSH2 PUBLIC KEY ----";

    private static final String SSH2_DSA_KEY = "ssh-dsa";

    private static final String SSH2_RSA_KEY = "ssh-rsa";

    private PublicKeyReaderUtil() {
    }

    public static PublicKey load(final String key) throws PublicKeyParseException {
        final int c = key.charAt(0);

        final String base64;

        if (c == 's') {
            base64 = PublicKeyReaderUtil.extractOpenSSHBase64(key);
        } else if (c == '-') {
            base64 = PublicKeyReaderUtil.extractSecSHBase64(key);
        } else {
            throw new PublicKeyParseException(PublicKeyParseException.ErrorCode.UNKNOWN_PUBLIC_KEY_FILE_FORMAT);
        }

        final SSH2DataBuffer buf = new SSH2DataBuffer(Base64.decodeBase64(base64.getBytes()));
        final String type = buf.readString();
        final PublicKey ret;
        if (PublicKeyReaderUtil.SSH2_DSA_KEY.equals(type)) {
            ret = decodeDSAPublicKey(buf);
        } else if (PublicKeyReaderUtil.SSH2_RSA_KEY.equals(type)) {
            ret = decodePublicKey(buf);
        } else {
            throw new PublicKeyParseException(PublicKeyParseException.ErrorCode.UNKNOWN_PUBLIC_KEY_CERTIFICATE_FORMAT);
        }

        return ret;
    }

    public static PublicKey loadOpenSsh(final String key) throws PublicKeyParseException {
        final int c = key.charAt(0);

        final String base64;

        if (c == 's') {
            base64 = PublicKeyReaderUtil.extractOpenSSHBase64(key);
        } else {
            throw new PublicKeyParseException(PublicKeyParseException.ErrorCode.UNKNOWN_PUBLIC_KEY_FILE_FORMAT);
        }

        final SSH2DataBuffer buf = new SSH2DataBuffer(Base64.decodeBase64(base64.getBytes()));
        final String type = buf.readString();
        final PublicKey ret;
        if (PublicKeyReaderUtil.SSH2_RSA_KEY.equals(type)) {
            ret = decodePublicKey(buf);
        } else {
            throw new PublicKeyParseException(PublicKeyParseException.ErrorCode.UNKNOWN_PUBLIC_KEY_CERTIFICATE_FORMAT);
        }

        return ret;
    }

    public static String extractOpenSSHBase64(final String key)
            throws PublicKeyParseException {
        final String base64;
        try {
            final StringTokenizer st = new StringTokenizer(key);
            st.nextToken();
            base64 = st.nextToken();
        } catch (final NoSuchElementException e) {
            throw new PublicKeyParseException(PublicKeyParseException.ErrorCode.CORRUPT_OPENSSH_PUBLIC_KEY_STRING);
        }

        return base64;
    }

    private static String extractSecSHBase64(final String key) throws PublicKeyParseException {
        final StringBuilder base64Data = new StringBuilder();

        boolean startKey = false;
        boolean startKeyBody = false;
        boolean endKey = false;
        boolean nextLineIsHeader = false;
        for (final String line : key.split("\n")) {
            final String trimLine = line.trim();
            if (!startKey && trimLine.equals(PublicKeyReaderUtil.BEGIN_PUB_KEY)) {
                startKey = true;
            } else if (startKey) {
                if (trimLine.equals(PublicKeyReaderUtil.END_PUB_KEY)) {
                    endKey = true;
                    break;
                } else if (nextLineIsHeader) {
                    if (!trimLine.endsWith("\\")) {
                        nextLineIsHeader = false;
                    }
                } else if (trimLine.indexOf(':') > 0) {
                    if (startKeyBody) {
                        throw new PublicKeyParseException(PublicKeyParseException.ErrorCode.CORRUPT_SECSSH_PUBLIC_KEY_STRING);
                    } else if (trimLine.endsWith("\\")) {
                        nextLineIsHeader = true;
                    }
                } else {
                    startKeyBody = true;
                    base64Data.append(trimLine);
                }
            }
        }

        if (!endKey) {
            throw new PublicKeyParseException(
                    PublicKeyParseException.ErrorCode.CORRUPT_SECSSH_PUBLIC_KEY_STRING);
        }

        return base64Data.toString();
    }

    private static PublicKey decodeDSAPublicKey(final SSH2DataBuffer buffer) throws PublicKeyParseException {
        final BigInteger p = buffer.readMPint();
        final BigInteger q = buffer.readMPint();
        final BigInteger g = buffer.readMPint();
        final BigInteger y = buffer.readMPint();

        try {
            final KeyFactory dsaKeyFact = KeyFactory.getInstance("DSA");
            final DSAPublicKeySpec dsaPubSpec = new DSAPublicKeySpec(y, p, q, g);

            return dsaKeyFact.generatePublic(dsaPubSpec);

        } catch (final Exception e) {
            throw new PublicKeyParseException(
                    PublicKeyParseException.ErrorCode.SSH2DSA_ERROR_DECODING_PUBLIC_KEY_BLOB, e);
        }
    }

    private static PublicKey decodePublicKey(final SSH2DataBuffer buffer) throws PublicKeyParseException {
        final BigInteger e = buffer.readMPint();
        final BigInteger n = buffer.readMPint();

        try {
            final KeyFactory rsaKeyFact = KeyFactory.getInstance("RSA");
            final RSAPublicKeySpec rsaPubSpec = new RSAPublicKeySpec(n, e);

            return rsaKeyFact.generatePublic(rsaPubSpec);

        } catch (final Exception ex) {
            throw new PublicKeyParseException(PublicKeyParseException.ErrorCode.SSH2RSA_ERROR_DECODING_PUBLIC_KEY_BLOB, ex);
        }
    }

    private static class SSH2DataBuffer {

        public static final int INT1 = 24;
        public static final int INT2 = 16;
        public static final int INT3 = 8;
        private final byte[] data;

        private int pos;

        public SSH2DataBuffer(final byte[] data) {
            this.data = data;
        }

        public BigInteger readMPint() throws PublicKeyParseException {
            final byte[] raw = readByteArray();
            return (raw.length > 0) ? new BigInteger(raw) : BigInteger.valueOf(0);
        }

        public String readString() throws PublicKeyParseException {
            return new String(readByteArray());
        }

        private int readUInt32() {
            final int byte1 = this.data[this.pos++];
            final int byte2 = this.data[this.pos++];
            final int byte3 = this.data[this.pos++];
            final int byte4 = this.data[this.pos++];
            return (byte1 << INT1) + (byte2 << INT2) + (byte3 << INT3) + (byte4 << 0);
        }

        private byte[] readByteArray() throws PublicKeyParseException {
            final int len = readUInt32();
            if ((len < 0) || (len > (this.data.length - this.pos))) {
                throw new PublicKeyParseException(
                        PublicKeyParseException.ErrorCode.CORRUPT_BYTE_ARRAY_ON_READ);
            }
            final byte[] str = new byte[len];
            System.arraycopy(this.data, this.pos, str, 0, len);
            this.pos += len;
            return str;
        }
    }

    public static final class PublicKeyParseException extends Exception {

        private final ErrorCode errorCode;

        private PublicKeyParseException(final ErrorCode errorCode) {
            super(errorCode.message);
            this.errorCode = errorCode;
        }

        private PublicKeyParseException(final ErrorCode errorCode,
                final Throwable cause) {
            super(errorCode.message, cause);
            this.errorCode = errorCode;
        }

        public ErrorCode getErrorCode() {
            return this.errorCode;
        }

        public enum ErrorCode {
            UNKNOWN_PUBLIC_KEY_FILE_FORMAT("Corrupt or unknown public key file format"),

            UNKNOWN_PUBLIC_KEY_CERTIFICATE_FORMAT("Corrupt or unknown public key certificate format"),

            CORRUPT_OPENSSH_PUBLIC_KEY_STRING("Corrupt OpenSSH public key string"),

            CORRUPT_SECSSH_PUBLIC_KEY_STRING("Corrupt SECSSH public key string"),

            SSH2DSA_ERROR_DECODING_PUBLIC_KEY_BLOB("SSH2DSA: error decoding public key blob"),

            SSH2RSA_ERROR_DECODING_PUBLIC_KEY_BLOB("SSH2RSA: error decoding public key blob"),

            CORRUPT_BYTE_ARRAY_ON_READ("Corrupt byte array on read");

            private final String message;

            ErrorCode(final String message) {
                this.message = message;
            }
        }
    }
}