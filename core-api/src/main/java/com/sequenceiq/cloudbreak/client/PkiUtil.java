package com.sequenceiq.cloudbreak.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.signers.PSSSigner;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.BaseEncoding;

public class PkiUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(PkiUtil.class);

    private static final int KEY_SIZE = 2048;

    private static final int CERT_VALIDITY_YEAR = 10;

    private static final Integer SALT_LENGTH = 20;

    private static final Integer MAX_CACHE_SIZE = 200;

    private static final Map<String, RSAKeyParameters> CACHE =
            Collections.synchronizedMap(new LinkedHashMap<>(MAX_CACHE_SIZE * 4 / 3, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, RSAKeyParameters> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            });

    private PkiUtil() {
    }

    public static byte[] getPublicKeyDer(String privateKeyPem) {
        try (PEMParser pemParser = new PEMParser(new StringReader(clarifyPemKey(privateKeyPem)))) {
            PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();
            return pemKeyPair.getPublicKeyInfo().getEncoded();
        } catch (IOException e) {
            throw new SecurityException(e);
        }
    }

    public static String generateSignature(String privateKeyPem, byte[] data) {
        RSAKeyParameters rsaKeyParameters = CACHE.get(privateKeyPem);

        if (rsaKeyParameters == null) {
            try (PEMParser pEMParser = new PEMParser(new StringReader(clarifyPemKey(privateKeyPem)))) {
                PEMKeyPair pemKeyPair = (PEMKeyPair) pEMParser.readObject();

                KeyFactory factory = KeyFactory.getInstance("RSA");
                KeySpec publicKeySpec = new X509EncodedKeySpec(pemKeyPair.getPublicKeyInfo().getEncoded());
                PublicKey publicKey = factory.generatePublic(publicKeySpec);
                KeySpec privateKeySpec = new PKCS8EncodedKeySpec(pemKeyPair.getPrivateKeyInfo().getEncoded());
                PrivateKey privateKey = factory.generatePrivate(privateKeySpec);
                KeyPair kp = new KeyPair(publicKey, privateKey);
                RSAPrivateKeySpec privKeySpec = factory.getKeySpec(kp.getPrivate(), RSAPrivateKeySpec.class);
                rsaKeyParameters = new RSAKeyParameters(true, privKeySpec.getModulus(), privKeySpec.getPrivateExponent());

                CACHE.put(privateKeyPem, rsaKeyParameters);
            } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException e) {
                throw new SecurityException(e);
            }
        }

        Signer signer = new PSSSigner(new RSAEngine(), new SHA256Digest(), SALT_LENGTH);
        signer.init(true, rsaKeyParameters);
        signer.update(data, 0, data.length);
        try {
            byte[] signature = signer.generateSignature();
            return BaseEncoding.base64().encode(signature);
        } catch (CryptoException e) {
            throw new SecurityException(e);
        }
    }

    public static KeyPair generateKeypair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(KEY_SIZE, new SecureRandom());
            return keyGen.generateKeyPair();

        } catch (Exception e) {
            throw new PkiException("Failed to generate PK for the cluster!", e);
        }
    }

    public static X509Certificate cert(KeyPair identity, String publicAddress, KeyPair signKey) {
        try {
            PKCS10CertificationRequest csr = generateCsr(identity, publicAddress);
            return selfsign(csr, publicAddress, signKey);

        } catch (Exception e) {
            throw new PkiException("Failed to create signed cert for the cluster!", e);
        }
    }

    public static String convert(PrivateKey privateKey) {
        try {
            return convertToString(privateKey);
        } catch (Exception e) {
            throw new PkiException("Failed to convert Private Key for the cluster!", e);
        }
    }

    public static String convert(PublicKey publicKey) {
        try {
            return convertToString(publicKey);
        } catch (Exception e) {
            throw new PkiException("Failed to convert Public Key for the cluster!", e);
        }
    }

    public static String convert(X509Certificate cert) {
        try {
            return convertToString(cert);
        } catch (Exception e) {
            throw new PkiException("Failed to convert signed cert to String", e);
        }
    }

    public static String convertOpenSshPublicKey(PublicKey publicKey) {
        ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(byteOs)) {
            RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
            dos.writeInt("ssh-rsa".getBytes().length);
            dos.write("ssh-rsa".getBytes());
            dos.writeInt(rsaPublicKey.getPublicExponent().toByteArray().length);
            dos.write(rsaPublicKey.getPublicExponent().toByteArray());
            dos.writeInt(rsaPublicKey.getModulus().toByteArray().length);
            dos.write(rsaPublicKey.getModulus().toByteArray());
            return "ssh-rsa " + new String(Base64.encodeBase64(byteOs.toByteArray()));
        } catch (Exception e) {
            throw new PkiException("Failed to convert public key for the cluster!", e);
        } finally {
            try {
                byteOs.close();
            } catch (IOException e) {
                LOGGER.debug("Failed to close streams while converting public key", e);
            }
        }
    }

    private static X509Certificate selfsign(PKCS10CertificationRequest inputCSR, String publicAddress, KeyPair signKey)
            throws Exception {

        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder()
                .find("SHA256withRSA");
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder()
                .find(sigAlgId);

        AsymmetricKeyParameter akp = PrivateKeyFactory.createKey(signKey.getPrivate()
                .getEncoded());

        Calendar cal = Calendar.getInstance();
        Date currentTime = cal.getTime();
        cal.add(Calendar.YEAR, CERT_VALIDITY_YEAR);
        Date expiryTime = cal.getTime();

        X509v3CertificateBuilder myCertificateGenerator = new X509v3CertificateBuilder(
                new X500Name(String.format("cn=%s", publicAddress)), new BigInteger("1"), currentTime, expiryTime, inputCSR.getSubject(),
                inputCSR.getSubjectPublicKeyInfo());

        ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
                .build(akp);

        X509CertificateHolder holder = myCertificateGenerator.build(sigGen);

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(holder.toASN1Structure().getEncoded()));
    }

    private static PKCS10CertificationRequest generateCsr(KeyPair identity, String publicAddress) throws Exception {
        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                new X500Principal(String.format("cn=%s", publicAddress)), identity.getPublic());
        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder("SHA256withRSA");
        ContentSigner signer = csBuilder.build(identity.getPrivate());
        return p10Builder.build(signer);
    }

    private static String convertToString(Object o) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStreamWriter output = new OutputStreamWriter(bos);
        try (JcaPEMWriter pem = new JcaPEMWriter(output)) {
            pem.writeObject(o);
        }
        return bos.toString();
    }

    private static String clarifyPemKey(String rawPem) {
        return "-----BEGIN RSA PRIVATE KEY-----\n" + rawPem.replaceAll("-----(.*)-----|\n", "") + "\n-----END RSA PRIVATE KEY-----";
    }
}
