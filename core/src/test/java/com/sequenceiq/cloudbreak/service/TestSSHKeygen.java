package com.sequenceiq.cloudbreak.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;

public class TestSSHKeygen {
    @Test
    public void testSSHKeyGen() throws IOException, JSchException {
        String publicKeyFilename = null;
        String privateKeyFilename = null;

        publicKeyFilename = "/tmp/bouncy.pub";
        privateKeyFilename = "/tmp/bouncy";

        JSch jsch=new JSch();
        KeyPair kpair= KeyPair.genKeyPair(jsch, KeyPair.RSA, 2048);
        kpair.writePrivateKey(privateKeyFilename);
        kpair.writePublicKey(publicKeyFilename, "sequence-eu");
        System.out.println("Finger print: " + kpair.getFingerPrint());

        kpair.dispose();
    }

    @Test
    public void testSSHConnect() throws IOException {

        final SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier("98:09:32:3c:66:00:01:ef:a0:15:b1:f2:7a:81:d1:44");

        ssh.connect("104.197.58.210", 22);
        ssh.authPublickey("cloudbreak", "/tmp/bouncy");
        final Session session = ssh.startSession();
        final Session.Command cmd = session.exec("ping -c 3 google.com");
        System.out.println(IOUtils.readFully(cmd.getInputStream()).toString());
        cmd.join(5, TimeUnit.SECONDS);
        System.out.println("\n** exit status: " + cmd.getExitStatus());
        session.close();
        ssh.disconnect();
    }

//    private void generate (String publicKeyFilename, String privateFilename){
//
//        try {
//
////            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
//
//            // Create the public and private keys
//            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", new org.bouncycastle.jce.provider.BouncyCastleProvider());
////            BASE64Encoder b64 = new BASE64Encoder();
//
////            SecureRandom random = new FixedRand();
//            generator.initialize(2048, new SecureRandom());
//
//            KeyPair pair = generator.genKeyPair();
//            RSAPublicKey pubKey = (RSAPublicKey) pair.getPublic();
//            RSAPrivateKey privKey = (RSAPrivateKey) pair.getPrivate();
//
//            byte[] pub = encodePublicKey(pubKey);
//            byte[] priv = passwordEncrypt("password".toCharArray(), privKey.getEncoded());
//            String publicKey1 = new String(Base64.encode(pub, 0, pub.length));
//            String privateKey = new String(Base64.encode(priv, 0,priv.length));
//
//            System.out.println("publicKey : " + publicKey1);
//            System.out.println("privateKey : " + privateKey);
//
//            BufferedWriter out = new BufferedWriter(new FileWriter(publicKeyFilename));
//            out.write("ssh-rsa " + new String(publicKey1));
//            out.close();
////
//            out = new BufferedWriter(new FileWriter(privateFilename));
//            out.write("-----BEGIN RSA PRIVATE KEY-----\n");
//            out.write(privateKey);
//            out.write("\n-----END RSA PRIVATE KEY-----\n");
//            out.close();
//
//
//        }
//        catch (Exception e) {
//            System.out.println(e);
//        }
//    }
//
//    public byte[] encodePublicKey(RSAPublicKey key) throws IOException
//    {
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//       /* encode the "ssh-rsa" string */
//        byte[] sshrsa = new byte[] {0, 0, 0, 7, 's', 's', 'h', '-', 'r', 's', 'a'};
//        out.write(sshrsa);
//       /* Encode the public exponent */
//        BigInteger e = key.getPublicExponent();
//        byte[] data = e.toByteArray();
//        encodeUInt32(data.length, out);
//        out.write(data);
//       /* Encode the modulus */
//        BigInteger m = key.getModulus();
//        data = m.toByteArray();
//        encodeUInt32(data.length, out);
//        out.write(data);
//        return out.toByteArray();
//    }
//
//    public void encodeUInt32(int value, OutputStream out) throws IOException
//    {
//        byte[] tmp = new byte[4];
//        tmp[0] = (byte)((value >>> 24) & 0xff);
//        tmp[1] = (byte)((value >>> 16) & 0xff);
//        tmp[2] = (byte)((value >>> 8) & 0xff);
//        tmp[3] = (byte)(value & 0xff);
//        out.write(tmp);
//    }
//
//    private static byte[] passwordEncrypt(char[] password, byte[] plaintext) throws Exception {
//        int MD5_ITERATIONS = 1000;
//        byte[] salt = new byte[8];
//        SecureRandom random = new SecureRandom();
//        random.nextBytes(salt);
//        PBEKeySpec keySpec = new PBEKeySpec(password);
//        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("org.bouncycastle.jcajce.provider.symmetric.PBEPBKDF2",new org.bouncycastle.jce.provider.BouncyCastleProvider());
//        SecretKey key = keyFactory.generateSecret(keySpec);
//        PBEParameterSpec paramSpec = new PBEParameterSpec(salt, MD5_ITERATIONS);
//        Cipher cipher = Cipher.getInstance("org.bouncycastle.jcajce.provider.symmetric.PBEPBKDF2",new org.bouncycastle.jce.provider.BouncyCastleProvider());
//        cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
//        byte[] ciphertext = cipher.doFinal(plaintext);
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        baos.write(salt);
//        baos.write(ciphertext);
//        return baos.toByteArray();
//    }
}
