package com.sequenceiq.cloudbreak.service.credential;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.commons.codec.binary.Base64;

import com.sequenceiq.cloudbreak.service.credential.PublicKeyReaderUtil.PublicKeyParseException.ErrorCode;

public final class PublicKeyReaderUtil {
    private static final String BEGIN_PUB_KEY = "---- BEGIN SSH2 PUBLIC KEY ----";

    private static final String END_PUB_KEY = "---- END SSH2 PUBLIC KEY ----";

    private static final String SSH2_DSA_KEY = "ssh-dsa";

    private static final String SSH2_RSA_KEY = "ssh-rsa";

    private PublicKeyReaderUtil() {
    }

    public static PublicKey load(String key) throws PublicKeyParseException {
        int c = key.charAt(0);

        String base64;

        if (c == 's') {
            base64 = extractOpenSSHBase64(key);
        } else if (c == '-') {
            base64 = extractSecSHBase64(key);
        } else {
            throw new PublicKeyParseException(ErrorCode.UNKNOWN_PUBLIC_KEY_FILE_FORMAT);
        }

        SSH2DataBuffer buf = new SSH2DataBuffer(Base64.decodeBase64(base64.getBytes()));
        String type = buf.readString();
        PublicKey ret;
        if (SSH2_DSA_KEY.equals(type)) {
            ret = decodeDSAPublicKey(buf);
        } else if (SSH2_RSA_KEY.equals(type)) {
            ret = decodePublicKey(buf);
        } else {
            throw new PublicKeyParseException(ErrorCode.UNKNOWN_PUBLIC_KEY_CERTIFICATE_FORMAT);
        }

        return ret;
    }

    public static PublicKey loadOpenSsh(String key) throws PublicKeyParseException {
        int c = key.charAt(0);

        String base64;

        if (c == 's') {
            base64 = extractOpenSSHBase64(key);
        } else {
            throw new PublicKeyParseException(ErrorCode.UNKNOWN_PUBLIC_KEY_FILE_FORMAT);
        }

        SSH2DataBuffer buf = new SSH2DataBuffer(Base64.decodeBase64(base64.getBytes()));
        String type = buf.readString();
        PublicKey ret;
        if (SSH2_RSA_KEY.equals(type)) {
            ret = decodePublicKey(buf);
        } else {
            throw new PublicKeyParseException(ErrorCode.UNKNOWN_PUBLIC_KEY_CERTIFICATE_FORMAT);
        }

        return ret;
    }

    public static String extractOpenSSHBase64(String key)
            throws PublicKeyParseException {
        String base64;
        try {
            StringTokenizer st = new StringTokenizer(key);
            st.nextToken();
            base64 = st.nextToken();
        } catch (NoSuchElementException ignored) {
            throw new PublicKeyParseException(ErrorCode.CORRUPT_OPENSSH_PUBLIC_KEY_STRING);
        }

        return base64;
    }

    private static String extractSecSHBase64(String key) throws PublicKeyParseException {
        StringBuilder base64Data = new StringBuilder();

        boolean startKey = false;
        boolean startKeyBody = false;
        boolean endKey = false;
        boolean nextLineIsHeader = false;
        for (String line : key.split("\n")) {
            String trimLine = line.trim();
            if (!startKey && trimLine.equals(BEGIN_PUB_KEY)) {
                startKey = true;
            } else if (startKey) {
                if (trimLine.equals(END_PUB_KEY)) {
                    endKey = true;
                    break;
                } else if (nextLineIsHeader) {
                    if (!trimLine.endsWith("\\")) {
                        nextLineIsHeader = false;
                    }
                } else if (trimLine.indexOf(':') > 0) {
                    if (startKeyBody) {
                        throw new PublicKeyParseException(ErrorCode.CORRUPT_SECSSH_PUBLIC_KEY_STRING);
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
                    ErrorCode.CORRUPT_SECSSH_PUBLIC_KEY_STRING);
        }

        return base64Data.toString();
    }

    private static PublicKey decodeDSAPublicKey(SSH2DataBuffer buffer) throws PublicKeyParseException {
        BigInteger p = buffer.readMPint();
        BigInteger q = buffer.readMPint();
        BigInteger g = buffer.readMPint();
        BigInteger y = buffer.readMPint();

        try {
            KeyFactory dsaKeyFact = KeyFactory.getInstance("DSA");
            KeySpec dsaPubSpec = new DSAPublicKeySpec(y, p, q, g);

            return dsaKeyFact.generatePublic(dsaPubSpec);

        } catch (Exception e) {
            throw new PublicKeyParseException(
                    ErrorCode.SSH2DSA_ERROR_DECODING_PUBLIC_KEY_BLOB, e);
        }
    }

    private static PublicKey decodePublicKey(SSH2DataBuffer buffer) throws PublicKeyParseException {
        BigInteger e = buffer.readMPint();
        BigInteger n = buffer.readMPint();

        try {
            KeyFactory rsaKeyFact = KeyFactory.getInstance("RSA");
            KeySpec rsaPubSpec = new RSAPublicKeySpec(n, e);

            return rsaKeyFact.generatePublic(rsaPubSpec);

        } catch (Exception ex) {
            throw new PublicKeyParseException(ErrorCode.SSH2RSA_ERROR_DECODING_PUBLIC_KEY_BLOB, ex);
        }
    }

    private static class SSH2DataBuffer {

        public static final int INT1 = 24;

        public static final int INT2 = 16;

        public static final int INT3 = 8;

        private final byte[] data;

        private int pos;

        SSH2DataBuffer(byte[] data) {
            this.data = data;
        }

        public BigInteger readMPint() throws PublicKeyParseException {
            byte[] raw = readByteArray();
            return (raw.length > 0) ? new BigInteger(raw) : BigInteger.valueOf(0);
        }

        public String readString() throws PublicKeyParseException {
            return new String(readByteArray());
        }

        private int readUInt32() {
            int byte1 = data[pos++];
            int byte2 = data[pos++];
            int byte3 = data[pos++];
            int byte4 = data[pos++];
            return (byte1 << INT1) + (byte2 << INT2) + (byte3 << INT3) + byte4;
        }

        private byte[] readByteArray() throws PublicKeyParseException {
            int len = readUInt32();
            if ((len < 0) || (len > (data.length - pos))) {
                throw new PublicKeyParseException(
                        ErrorCode.CORRUPT_BYTE_ARRAY_ON_READ);
            }
            byte[] str = new byte[len];
            System.arraycopy(data, pos, str, 0, len);
            pos += len;
            return str;
        }
    }

    public static final class PublicKeyParseException extends Exception {

        private final ErrorCode errorCode;

        private PublicKeyParseException(ErrorCode errorCode) {
            super(errorCode.message);
            this.errorCode = errorCode;
        }

        private PublicKeyParseException(ErrorCode errorCode,
                Throwable cause) {
            super(errorCode.message, cause);
            this.errorCode = errorCode;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        public enum ErrorCode {
            UNKNOWN_PUBLIC_KEY_FILE_FORMAT("Corrupt or unknown public key file format"),

            UNKNOWN_PUBLIC_KEY_CERTIFICATE_FORMAT("Corrupt or unknown public key certificate format"),

            CORRUPT_OPENSSH_PUBLIC_KEY_STRING("Corrupt OpenSSH public key string"),

            CORRUPT_SECSSH_PUBLIC_KEY_STRING("Corrupt SECSSH public key string"),

            SSH2DSA_ERROR_DECODING_PUBLIC_KEY_BLOB("SSH2DSA: error decoding public key blob"),

            SSH2RSA_ERROR_DECODING_PUBLIC_KEY_BLOB("SSH2RSA: error decoding public key blob"),

            CORRUPT_BYTE_ARRAY_ON_READ("Public key length is shorter than 2048 bits or byte array is corrupt.");

            private final String message;

            ErrorCode(String message) {
                this.message = message;
            }
        }
    }
}