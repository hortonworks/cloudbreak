package com.sequenceiq.cloudbreak.util;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.EdECPoint;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.StringTokenizer;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.util.PublicKeyReaderUtil.PublicKeyParseException.ErrorCode;

public final class PublicKeyReaderUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicKeyReaderUtil.class);

    private static final String BEGIN_PUB_KEY = "---- BEGIN SSH2 PUBLIC KEY ----";

    private static final String END_PUB_KEY = "---- END SSH2 PUBLIC KEY ----";

    private static final String SSH2_RSA_KEY = "ssh-rsa";

    private static final String SSH2_ED25519_KEY = "ssh-ed25519";

    private static final int NUM_BYTES_ED25519 = 32;

    private static final int MIN_RSA_KEY_BITS = 2048;

    private PublicKeyReaderUtil() {
    }

    /**
     * Checks only the RSA key length, any other problem with the key is logged and ignored, because it is reported by other validations.
     *
     * @return the error message if the key is an RSA key shorter than the minimum required by RHEL 9, empty otherwise
     */
    public static Optional<String> findTooShortRsaKeyMessage(String key) {
        try {
            // FIPS mode only restricts Ed25519 keys, which never fail the RSA length check
            load(key, false);
        } catch (PublicKeyParseException e) {
            if (e.getErrorCode() == ErrorCode.RSA_KEY_TOO_SHORT) {
                return Optional.of(e.getMessage());
            }
            LOGGER.info("Ignoring SSH public key parse problem during RSA key length check: {}", e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.info("Ignoring SSH public key problem during RSA key length check", e);
        }
        return Optional.empty();
    }

    public static PublicKey load(String key, boolean fipsEnabled) throws PublicKeyParseException {
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

        return switch (type) {
            case SSH2_RSA_KEY -> decodeRsaPublicKey(buf);
            case SSH2_ED25519_KEY -> {
                if (fipsEnabled) {
                    throw new PublicKeyParseException(ErrorCode.SSH2ED25519_FORBIDDEN_IN_FIPS_MODE);
                }
                yield decodeEd25519PublicKey(buf);
            }
            default -> throw new PublicKeyParseException(ErrorCode.UNKNOWN_PUBLIC_KEY_CERTIFICATE_FORMAT);
        };
    }

    private static String extractOpenSSHBase64(String key) throws PublicKeyParseException {
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
            throw new PublicKeyParseException(ErrorCode.CORRUPT_SECSSH_PUBLIC_KEY_STRING);
        }

        return base64Data.toString();
    }

    private static PublicKey decodeRsaPublicKey(SSH2DataBuffer buffer) throws PublicKeyParseException {
        BigInteger e = buffer.readMPint();
        BigInteger n = buffer.readMPint();

        // RHEL 9 OpenSSH (RequiredRSASize 2048) rejects e.g. 2047-bit moduli, so reject them before any provider-specific checks
        if (n.bitLength() < MIN_RSA_KEY_BITS) {
            throw new PublicKeyParseException(ErrorCode.RSA_KEY_TOO_SHORT,
                    String.format("RSA public key is %d bits, at least %d bits are required", n.bitLength(), MIN_RSA_KEY_BITS));
        }

        try {
            KeyFactory rsaKeyFact = KeyFactory.getInstance("RSA");
            KeySpec rsaPubSpec = new RSAPublicKeySpec(n, e);

            return rsaKeyFact.generatePublic(rsaPubSpec);
        } catch (Exception ex) {
            LOGGER.debug("Failed to decode RSA public key", ex);
            throw new PublicKeyParseException(ErrorCode.SSH2RSA_ERROR_DECODING_PUBLIC_KEY_BLOB, ex);
        }
    }

    private static PublicKey decodeEd25519PublicKey(SSH2DataBuffer buffer) throws PublicKeyParseException {

        try {
            AlgorithmIdentifier ed25519AlgId = new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519);
            SubjectPublicKeyInfo spki = new SubjectPublicKeyInfo(ed25519AlgId, buffer.readByteArray());
            byte[] spkiEncodedBytes = spki.getEncoded("DER");
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(spkiEncodedBytes);
            return keyFactory.generatePublic(keySpec);
        } catch (Exception ex) {
            LOGGER.debug("Failed to decode ed25519 public key", ex);
            throw new PublicKeyParseException(ErrorCode.SSH2ED25519_ERROR_DECODING_PUBLIC_KEY_BLOB, ex);
        }
    }

    private static class SSH2DataBuffer {

        private static final int INT1 = 24;

        private static final int INT2 = 16;

        private static final int INT3 = 8;

        private static final int U_BYTE_MAX_VALUE = 0xFF;

        private static final int NUM_BITS_7 = 7;

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

        public EdECPoint readEdEcPoint() throws PublicKeyParseException {
            byte[] raw = readByteArray();
            if (raw.length != NUM_BYTES_ED25519) {
                throw new PublicKeyParseException(ErrorCode.MALFORMED_ED25519_ED_EC_POINT);
            }
            int lastByte = raw[raw.length - 1] & U_BYTE_MAX_VALUE;
            boolean xOdd = lastByte >> NUM_BITS_7 == 1;
            raw[raw.length - 1] &= Byte.MAX_VALUE;
            // The input is encoded as little-endian, whereas BigInteger expects big-endian.
            ArrayUtils.reverse(raw);
            BigInteger y = new BigInteger(1, raw);
            return new EdECPoint(xOdd, y);
        }

        private int readUInt32BigEndian() {
            int byte1 = data[pos++] & U_BYTE_MAX_VALUE;
            int byte2 = data[pos++] & U_BYTE_MAX_VALUE;
            int byte3 = data[pos++] & U_BYTE_MAX_VALUE;
            int byte4 = data[pos++] & U_BYTE_MAX_VALUE;
            return (byte1 << INT1) + (byte2 << INT2) + (byte3 << INT3) + byte4;
        }

        private byte[] readByteArray() throws PublicKeyParseException {
            int len = readUInt32BigEndian();
            if (len < 0 || len > (data.length - pos)) {
                throw new PublicKeyParseException(ErrorCode.CORRUPT_BYTE_ARRAY_ON_READ);
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

        private PublicKeyParseException(ErrorCode errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        private PublicKeyParseException(ErrorCode errorCode, Throwable cause) {
            super(errorCode.message + ": " + cause.getMessage(), cause);
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

            SSH2RSA_ERROR_DECODING_PUBLIC_KEY_BLOB("SSH2RSA: error decoding public key blob"),

            SSH2ED25519_FORBIDDEN_IN_FIPS_MODE("SSH2ED25519: this key type is not allowed when running clusters in FIPS mode"),

            SSH2ED25519_ERROR_DECODING_PUBLIC_KEY_BLOB("SSH2ED25519: error decoding public key blob"),

            CORRUPT_BYTE_ARRAY_ON_READ("Public key byte array is corrupt."),

            RSA_KEY_TOO_SHORT("RSA public key is too short"),

            MALFORMED_ED25519_ED_EC_POINT("SSH2ED25519: byte array for EdECPoint must be " + NUM_BYTES_ED25519 + " bytes long");

            private final String message;

            ErrorCode(String message) {
                this.message = message;
            }
        }
    }
}
