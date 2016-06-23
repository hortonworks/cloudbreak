package com.sequenceiq.cloudbreak.service.credential

import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.DSAPublicKeySpec
import java.security.spec.RSAPublicKeySpec
import java.util.NoSuchElementException
import java.util.StringTokenizer

import org.apache.commons.codec.binary.Base64

object PublicKeyReaderUtil {
    private val BEGIN_PUB_KEY = "---- BEGIN SSH2 PUBLIC KEY ----"

    private val END_PUB_KEY = "---- END SSH2 PUBLIC KEY ----"

    private val SSH2_DSA_KEY = "ssh-dsa"

    private val SSH2_RSA_KEY = "ssh-rsa"

    @Throws(PublicKeyParseException::class)
    fun load(key: String): PublicKey {
        val c = key[0].toInt()

        val base64: String

        if (c == 's') {
            base64 = PublicKeyReaderUtil.extractOpenSSHBase64(key)
        } else if (c == '-') {
            base64 = PublicKeyReaderUtil.extractSecSHBase64(key)
        } else {
            throw PublicKeyParseException(PublicKeyParseException.ErrorCode.UNKNOWN_PUBLIC_KEY_FILE_FORMAT)
        }

        val buf = SSH2DataBuffer(Base64.decodeBase64(base64.toByteArray()))
        val type = buf.readString()
        val ret: PublicKey
        if (PublicKeyReaderUtil.SSH2_DSA_KEY == type) {
            ret = decodeDSAPublicKey(buf)
        } else if (PublicKeyReaderUtil.SSH2_RSA_KEY == type) {
            ret = decodePublicKey(buf)
        } else {
            throw PublicKeyParseException(PublicKeyParseException.ErrorCode.UNKNOWN_PUBLIC_KEY_CERTIFICATE_FORMAT)
        }

        return ret
    }

    @Throws(PublicKeyParseException::class)
    fun loadOpenSsh(key: String): PublicKey {
        val c = key[0].toInt()

        val base64: String

        if (c == 's') {
            base64 = PublicKeyReaderUtil.extractOpenSSHBase64(key)
        } else {
            throw PublicKeyParseException(PublicKeyParseException.ErrorCode.UNKNOWN_PUBLIC_KEY_FILE_FORMAT)
        }

        val buf = SSH2DataBuffer(Base64.decodeBase64(base64.toByteArray()))
        val type = buf.readString()
        val ret: PublicKey
        if (PublicKeyReaderUtil.SSH2_RSA_KEY == type) {
            ret = decodePublicKey(buf)
        } else {
            throw PublicKeyParseException(PublicKeyParseException.ErrorCode.UNKNOWN_PUBLIC_KEY_CERTIFICATE_FORMAT)
        }

        return ret
    }

    @Throws(PublicKeyParseException::class)
    fun extractOpenSSHBase64(key: String): String {
        val base64: String
        try {
            val st = StringTokenizer(key)
            st.nextToken()
            base64 = st.nextToken()
        } catch (e: NoSuchElementException) {
            throw PublicKeyParseException(PublicKeyParseException.ErrorCode.CORRUPT_OPENSSH_PUBLIC_KEY_STRING)
        }

        return base64
    }

    @Throws(PublicKeyParseException::class)
    private fun extractSecSHBase64(key: String): String {
        val base64Data = StringBuilder()

        var startKey = false
        var startKeyBody = false
        var endKey = false
        var nextLineIsHeader = false
        for (line in key.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
            val trimLine = line.trim({ it <= ' ' })
            if (!startKey && trimLine == PublicKeyReaderUtil.BEGIN_PUB_KEY) {
                startKey = true
            } else if (startKey) {
                if (trimLine == PublicKeyReaderUtil.END_PUB_KEY) {
                    endKey = true
                    break
                } else if (nextLineIsHeader) {
                    if (!trimLine.endsWith("\\")) {
                        nextLineIsHeader = false
                    }
                } else if (trimLine.indexOf(':') > 0) {
                    if (startKeyBody) {
                        throw PublicKeyParseException(PublicKeyParseException.ErrorCode.CORRUPT_SECSSH_PUBLIC_KEY_STRING)
                    } else if (trimLine.endsWith("\\")) {
                        nextLineIsHeader = true
                    }
                } else {
                    startKeyBody = true
                    base64Data.append(trimLine)
                }
            }
        }

        if (!endKey) {
            throw PublicKeyParseException(
                    PublicKeyParseException.ErrorCode.CORRUPT_SECSSH_PUBLIC_KEY_STRING)
        }

        return base64Data.toString()
    }

    @Throws(PublicKeyParseException::class)
    private fun decodeDSAPublicKey(buffer: SSH2DataBuffer): PublicKey {
        val p = buffer.readMPint()
        val q = buffer.readMPint()
        val g = buffer.readMPint()
        val y = buffer.readMPint()

        try {
            val dsaKeyFact = KeyFactory.getInstance("DSA")
            val dsaPubSpec = DSAPublicKeySpec(y, p, q, g)

            return dsaKeyFact.generatePublic(dsaPubSpec)

        } catch (e: Exception) {
            throw PublicKeyParseException(
                    PublicKeyParseException.ErrorCode.SSH2DSA_ERROR_DECODING_PUBLIC_KEY_BLOB, e)
        }

    }

    @Throws(PublicKeyParseException::class)
    private fun decodePublicKey(buffer: SSH2DataBuffer): PublicKey {
        val e = buffer.readMPint()
        val n = buffer.readMPint()

        try {
            val rsaKeyFact = KeyFactory.getInstance("RSA")
            val rsaPubSpec = RSAPublicKeySpec(n, e)

            return rsaKeyFact.generatePublic(rsaPubSpec)

        } catch (ex: Exception) {
            throw PublicKeyParseException(PublicKeyParseException.ErrorCode.SSH2RSA_ERROR_DECODING_PUBLIC_KEY_BLOB, ex)
        }

    }

    private class SSH2DataBuffer internal constructor(private val data: ByteArray) {

        private var pos: Int = 0

        @Throws(PublicKeyParseException::class)
        fun readMPint(): BigInteger {
            val raw = readByteArray()
            return if (raw.size > 0) BigInteger(raw) else BigInteger.valueOf(0)
        }

        @Throws(PublicKeyParseException::class)
        fun readString(): String {
            return String(readByteArray())
        }

        private fun readUInt32(): Int {
            val byte1 = this.data[this.pos++].toInt()
            val byte2 = this.data[this.pos++].toInt()
            val byte3 = this.data[this.pos++].toInt()
            val byte4 = this.data[this.pos++].toInt()
            return (byte1 shl INT1) + (byte2 shl INT2) + (byte3 shl INT3) + (byte4 shl 0)
        }

        @Throws(PublicKeyParseException::class)
        private fun readByteArray(): ByteArray {
            val len = readUInt32()
            if (len < 0 || len > this.data.size - this.pos) {
                throw PublicKeyParseException(
                        PublicKeyParseException.ErrorCode.CORRUPT_BYTE_ARRAY_ON_READ)
            }
            val str = ByteArray(len)
            System.arraycopy(this.data, this.pos, str, 0, len)
            this.pos += len
            return str
        }

        companion object {

            val INT1 = 24
            val INT2 = 16
            val INT3 = 8
        }
    }

    class PublicKeyParseException : Exception {

        val errorCode: ErrorCode

        private constructor(errorCode: ErrorCode) : super(errorCode.message) {
            this.errorCode = errorCode
        }

        private constructor(errorCode: ErrorCode,
                            cause: Throwable) : super(errorCode.message, cause) {
            this.errorCode = errorCode
        }

        enum class ErrorCode private constructor(private val message: String) {
            UNKNOWN_PUBLIC_KEY_FILE_FORMAT("Corrupt or unknown public key file format"),

            UNKNOWN_PUBLIC_KEY_CERTIFICATE_FORMAT("Corrupt or unknown public key certificate format"),

            CORRUPT_OPENSSH_PUBLIC_KEY_STRING("Corrupt OpenSSH public key string"),

            CORRUPT_SECSSH_PUBLIC_KEY_STRING("Corrupt SECSSH public key string"),

            SSH2DSA_ERROR_DECODING_PUBLIC_KEY_BLOB("SSH2DSA: error decoding public key blob"),

            SSH2RSA_ERROR_DECODING_PUBLIC_KEY_BLOB("SSH2RSA: error decoding public key blob"),

            CORRUPT_BYTE_ARRAY_ON_READ("Corrupt byte array on read")
        }
    }
}