package com.sequenceiq.cloudbreak.certificate;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.PSSParameterSpec;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.google.common.io.BaseEncoding;

public class PkiUtil {
    static final String SHA_256_WITH_RSA = "SHA256withRSA";

    static final Integer SALT_LENGTH = 20;

    private static final Logger LOGGER = LoggerFactory.getLogger(PkiUtil.class);

    private static final int KEY_SIZE = 2048;

    private static final int CERT_VALIDITY_YEAR = 10;

    private static final Integer MAX_CACHE_SIZE = 200;

    private static final int CSR_PRINT_INDEX = 64;

    private static final String EC_CURVE = "secp384r1";

    private static final String SHA_384_WITH_ECDSA = "SHA384withECDSA";

    private static final Map<String, PrivateKey> CACHE =
            Collections.synchronizedMap(new LinkedHashMap<>(MAX_CACHE_SIZE * 4 / 3, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, PrivateKey> eldest) {
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

    public static PublicKey getPublicKey(String privateKeyPem) {
        try (PEMParser pemParser = new PEMParser(new StringReader(clarifyPemKey(privateKeyPem)))) {
            PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();
            return new JcaPEMKeyConverter().getPublicKey(pemKeyPair.getPublicKeyInfo());
        } catch (IOException e) {
            throw new SecurityException(e);
        }
    }

    public static String generateSignature(String privateKeyPem, byte[] data) {
        PrivateKey privateKey = CACHE.get(privateKeyPem);

        if (privateKey == null) {
            try (PEMParser pemParser = new PEMParser(new StringReader(clarifyPemKey(privateKeyPem)))) {
                PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();

                KeyFactory factory = KeyFactory.getInstance("RSA");
                KeySpec privateKeySpec = new PKCS8EncodedKeySpec(pemKeyPair.getPrivateKeyInfo().getEncoded());
                privateKey = factory.generatePrivate(privateKeySpec);

                CACHE.put(privateKeyPem, privateKey);
            } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException e) {
                throw new SecurityException(e);
            }
        }

        try {
            Signature signature = Signature.getInstance("SHA256withRSAandMGF1");
            signature.initSign(privateKey);
            signature.setParameter(new PSSParameterSpec("SHA-256", "MGF1",
                    new MGF1ParameterSpec("SHA-256"), SALT_LENGTH, PSSParameterSpec.DEFAULT.getTrailerField()));
            signature.update(data);
            byte[] signedData = signature.sign();
            return BaseEncoding.base64().encode(signedData);
        } catch (GeneralSecurityException e) {
            LOGGER.warn("Failed to create signature", e);
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

    public static KeyPair generateEcdsaKeypair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BCFIPS");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec(EC_CURVE);
            keyGen.initialize(ecSpec, SecureRandom.getInstanceStrong());
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new PkiException("Failed to generate ECDSA PK for the cluster!", e);
        }
    }

    public static String generatePemPrivateKeyInBase64() {
        return BaseEncoding.base64().encode(convert(generateKeypair().getPrivate()).getBytes());
    }

    public static X509Certificate certByCsr(PKCS10CertificationRequest csr, String publicAddress, KeyPair signKey, int validity) {
        try {
            return selfsign(csr, publicAddress, signKey, validity);
        } catch (Exception e) {
            throw new PkiException("Failed to create signed cert for the cluster!", e);
        }
    }

    public static X509Certificate cert(KeyPair identity, String publicAddress, KeyPair signKey, int validity) {
        try {
            PKCS10CertificationRequest csr = generateCsr(identity, publicAddress);
            return selfsign(csr, publicAddress, signKey, validity);
        } catch (Exception e) {
            throw new PkiException("Failed to create signed cert for the cluster!", e);
        }
    }

    public static X509Certificate cert(KeyPair identity, String publicAddress, KeyPair signKey) {
        return cert(identity, publicAddress, signKey, CERT_VALIDITY_YEAR);
    }

    public static PKCS10CertificationRequest csr(KeyPair identity, String commonName, List<String> subjectAlternativeNames) {
        if (identity == null) {
            throw new PkiException("Failed to generate CSR because KeyPair hasn't been specified for the method!");
        }
        try {
            String name = String.format("C=US, CN=%s, O=Cloudera", commonName);
            LOGGER.info("Generate CSR with X.500 distinguished name: '{}' and list of SAN: '{}'", name, String.join(",", subjectAlternativeNames));
            return generateCsrWithName(identity, name, subjectAlternativeNames);
        } catch (Exception e) {
            throw new PkiException("Failed to generate csr for the cluster!", e);
        }
    }

    public static String convert(PrivateKey privateKey) {
        try {
            return convertToString(privateKey);
        } catch (Exception e) {
            throw new PkiException("Failed to convert Private Key for the cluster!", e);
        }
    }

    public static String convertEcPrivateKey(PrivateKey privateKey) {
        try {
            PrivateKeyInfo privKeyInfo = PrivateKeyInfo.getInstance(privateKey.getEncoded());
            byte[] privKeyEncoded = privKeyInfo.parsePrivateKey().toASN1Primitive().getEncoded();
            StringWriter stringWriter = new StringWriter();
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
                PemObject pemObject = new PemObject("EC PRIVATE KEY", privKeyEncoded);
                pemWriter.writeObject(pemObject);
            }
            return stringWriter.toString();

        } catch (Exception e) {
            throw new PkiException("Failed to convert Private Key for the cluster!", e);
        }
    }

    public static String convertPrivateKeyToPKCSPEM(PrivateKey privateKey) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            OutputStreamWriter output = new OutputStreamWriter(bos);
            try (JcaPEMWriter pem = new JcaPEMWriter(output)) {
                pem.writeObject(new JcaPKCS8Generator(privateKey, null));
            }
            return bos.toString();
        } catch (Exception e) {
            throw new PkiException("Failed to convert Private Key for the cluster!", e);
        }
    }

    public static String convertPemPublicKey(PublicKey publicKey) {
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

    public static KeyPair fromPrivateKeyPem(String privateKeyContent) {
        BufferedReader br = new BufferedReader(new StringReader(privateKeyContent));
        try (PEMParser pp = new PEMParser(br)) {
            PEMKeyPair pemKeyPair = (PEMKeyPair) pp.readObject();
            return new JcaPEMKeyConverter().getKeyPair(pemKeyPair);
        } catch (IOException e) {
            LOGGER.info("Cannot parse KeyPair from private key PEM content, skip it. {}", e.getMessage(), e);
            return null;
        }
    }

    public static X509Certificate fromCertificatePem(String certPem) {
        try (Reader reader = new StringReader(certPem)) {
            try (PEMParser pemParser = new PEMParser(reader)) {
                Object pemObject = pemParser.readObject();
                if (!(pemObject instanceof X509CertificateHolder)) {
                    throw new PkiException(String.format("Cannot parse X.509 certificate from PEM content. Expected \"%s\" object, got \"%s\".",
                            X509CertificateHolder.class.getName(), pemObject == null ? "null" : pemObject.getClass().getName()));
                }
                return new JcaX509CertificateConverter().getCertificate((X509CertificateHolder) pemObject);
            }
        } catch (PkiException pe) {
            throw pe;
        } catch (Exception e) {
            throw new PkiException("Cannot parse X.509 certificate from PEM content", e);
        }
    }

    public static String calculatePemPublicKeyInBase64(String pemPrivateKeyInBase64) {
        PublicKey publicKey = getPublicKey(new String(BaseEncoding.base64().decode(pemPrivateKeyInBase64)));
        String pemFormatPublicKey = convertPemPublicKey(publicKey);
        return BaseEncoding.base64().encode(pemFormatPublicKey.getBytes());
    }

    private static X509Certificate selfsign(PKCS10CertificationRequest inputCSR, String publicAddress, KeyPair signKey, int validity)
            throws Exception {
        Calendar cal = Calendar.getInstance();
        Date currentTime = cal.getTime();
        cal.add(Calendar.YEAR, validity);
        Date expiryTime = cal.getTime();

        X509v3CertificateBuilder myCertificateGenerator = new X509v3CertificateBuilder(
                new X500Name(String.format("cn=%s", publicAddress)), new BigInteger("1"), currentTime, expiryTime, inputCSR.getSubject(),
                inputCSR.getSubjectPublicKeyInfo());

        // Extract SANs from CSR and add to the certificate if present
        for (Attribute attribute : inputCSR.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
            Extensions extensions = Extensions.getInstance(attribute.getAttrValues().getObjectAt(0));
            Extension sanExtension = extensions.getExtension(Extension.subjectAlternativeName);
            if (sanExtension != null) {
                myCertificateGenerator.addExtension(Extension.subjectAlternativeName, false, sanExtension.getParsedValue());
            }
        }

        ContentSigner sigGen = new JcaContentSignerBuilder(getSignatureAlgorithmByPrivateKey(signKey.getPrivate())).build(signKey.getPrivate());
        X509CertificateHolder holder = myCertificateGenerator.build(sigGen);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(holder.toASN1Structure().getEncoded()));
    }

    static String getSignatureAlgorithmByPrivateKey(PrivateKey privateKey) {
        String algorithm = privateKey.getAlgorithm();
        return switch (algorithm) {
            case "EC", "ECDSA" -> SHA_384_WITH_ECDSA;
            default -> SHA_256_WITH_RSA;
        };
    }

    private static PKCS10CertificationRequest generateCsr(KeyPair identity, String publicAddress) throws Exception {
        String name = String.format("cn=%s", publicAddress);
        return generateCsrWithName(identity, name, null);
    }

    private static PKCS10CertificationRequest generateCsrWithName(KeyPair identity, String name, List<String> sanList) throws Exception {
        X500Principal principal = new X500Principal(name);
        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(principal, identity.getPublic());

        if (!CollectionUtils.isEmpty(sanList)) {
            p10Builder = addSubjectAlternativeNames(p10Builder, sanList);
        }

        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(getSignatureAlgorithmByPrivateKey(identity.getPrivate()));
        ContentSigner signer = csBuilder.build(identity.getPrivate());
        return p10Builder.build(signer);
    }

    private static PKCS10CertificationRequestBuilder addSubjectAlternativeNames(PKCS10CertificationRequestBuilder p10Builder, List<String> sanList)
            throws IOException {
        GeneralName[] generalNames = sanList
                .stream()
                .map(address -> new GeneralName(GeneralName.dNSName, address))
                .toArray(GeneralName[]::new);

        GeneralNames subjectAltNames = new GeneralNames(generalNames);
        ExtensionsGenerator extGen = new ExtensionsGenerator();
        extGen.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);
        return p10Builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
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

    public static String getPEMEncodedCSR(PKCS10CertificationRequest csr) throws Exception {
        byte[] csrBytes = csr.getEncoded();
        String encodedCSR = java.util.Base64.getEncoder().encodeToString(csrBytes);
        StringBuilder pemCSR = new StringBuilder();
        pemCSR.append("-----BEGIN CERTIFICATE REQUEST-----\n");
        int index = 0;
        while (index < encodedCSR.length()) {
            pemCSR.append(encodedCSR, index, Math.min(index + CSR_PRINT_INDEX, encodedCSR.length()));
            pemCSR.append("\n");
            index += CSR_PRINT_INDEX;
        }
        pemCSR.append("-----END CERTIFICATE REQUEST-----\n");
        return pemCSR.toString();
    }

}
