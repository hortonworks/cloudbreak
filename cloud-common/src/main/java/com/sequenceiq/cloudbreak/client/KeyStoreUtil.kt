package com.sequenceiq.cloudbreak.client

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser

class KeyStoreUtil @Throws(IllegalAccessException::class)
private constructor() {

    init {
        throw IllegalAccessException("KeyStoreUtil could not be initalized")
    }

    companion object {

        @Throws(Exception::class)
        fun createKeyStore(clientCertPath: String, clientKeyPath: String): KeyStore {
            val keyPair = loadPrivateKey(clientKeyPath)
            val privateCertificate = loadCertificate(clientCertPath)

            val keyStore = KeyStore.getInstance("JKS")
            keyStore.load(null)

            val cert = arrayOf(privateCertificate)
            keyStore.setKeyEntry("client", keyPair.private, "consul".toCharArray(), cert)
            return keyStore
        }

        @Throws(Exception::class)
        fun createTrustStore(serverCertPath: String): KeyStore {
            val serverCertFile = File(serverCertPath)
            val reader = BufferedReader(FileReader(serverCertFile))
            var pemParser: PEMParser? = null

            try {
                pemParser = PEMParser(reader)
                val certificateHolder = pemParser.readObject() as X509CertificateHolder
                val caCertificate = JcaX509CertificateConverter().getCertificate(certificateHolder)

                val trustStore = KeyStore.getInstance("JKS")
                trustStore.load(null)
                trustStore.setCertificateEntry("ca", caCertificate)
                return trustStore

            } finally {
                if (pemParser != null) {
                    pemParser.close()
                }

                if (reader != null) {
                    pemParser!!.close()
                }
            }
        }

        @Throws(IOException::class, CertificateException::class)
        private fun loadCertificate(certPath: String): Certificate {
            val certificate = File(certPath)
            val reader = BufferedReader(FileReader(certificate))
            var pemParser: PEMParser? = null

            try {
                pemParser = PEMParser(reader)
                val certificateHolder = pemParser.readObject() as X509CertificateHolder
                return JcaX509CertificateConverter().getCertificate(certificateHolder)
            } finally {
                if (pemParser != null) {
                    pemParser.close()
                }

                if (reader != null) {
                    pemParser!!.close()
                }
            }

        }

        @Throws(IOException::class, InvalidKeySpecException::class, NoSuchAlgorithmException::class)
        private fun loadPrivateKey(clientKeyPath: String): KeyPair {
            val privateKeyFile = File(clientKeyPath)
            val reader = BufferedReader(FileReader(privateKeyFile))

            var pemParser: PEMParser? = null

            try {
                pemParser = PEMParser(reader)

                val pemKeyPair = pemParser.readObject() as PEMKeyPair

                val pemPrivateKeyEncoded = pemKeyPair.privateKeyInfo.encoded
                val pemPublicKeyEncoded = pemKeyPair.publicKeyInfo.encoded

                val factory = KeyFactory.getInstance("RSA")

                val publicKeySpec = X509EncodedKeySpec(pemPublicKeyEncoded)
                val publicKey = factory.generatePublic(publicKeySpec)

                val privateKeySpec = PKCS8EncodedKeySpec(pemPrivateKeyEncoded)
                val privateKey = factory.generatePrivate(privateKeySpec)

                return KeyPair(publicKey, privateKey)

            } finally {
                if (pemParser != null) {
                    pemParser.close()
                }

                if (reader != null) {
                    pemParser!!.close()
                }
            }

        }
    }
}
