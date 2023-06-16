package com.sequenceiq.cloudbreak.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.security.PublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EdECPoint;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.util.PublicKeyReaderUtil.PublicKeyParseException;

class PublicKeyReaderUtilTest {

    @Test
    void loadTestRsaKeyMustUseAlgorithmRSAWhenOpenSSH() throws PublicKeyParseException {
        PublicKey rsaKey = PublicKeyReaderUtil.load("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQDfz7EED7gE8qbDKg6tUMtL3yW/" +
                "W1kiGzWBbVhOMiFYjnVje8mNF5jNe8rj+tg6fKA9dMpULrTIaFC5Dbhq4c0vCRqTbzrPkrXCURAdWx1lb+NzfTIOqLVeUZlnh" +
                "NPXBPVss+pnkawsI4HUiMRuScQnzrbvY9HcLhb69Qe9sZvSx1R46F9BEOZduX3QJxV2NBer6zJAZxv/YJt/ObzSFSnWN5isG+X" +
                "x/tIUZHighuqH7k0pzanp8IH11O6qXuCyqk9c7110w/vlQ+N8N3UhtAOhPCljKjYJo0KgF9C//l9eVKi/QICGVbr6vzvAyiWg" +
                "uf1lMHUEmsDlGQqfq/nnKCQYlz2V6oOaPt6ggmdmNCppFOSXri0dm1X4TnLASpIFetdjFGTU+sH75SmSrPlYn5HOxcqeVGgHNIZ2" +
                "taY+u12DWtY7cqFXvHE6pFmPme2Z0wQrAq/rH2BqGl3pkWVDIItbtsVsVTlS6E1Y/dXJkjaBiBZTc7aeBLa58oj5F4JYb" +
                "sc= test@test.local", false);
        assertEquals("RSA", rsaKey.getAlgorithm());
        if (rsaKey instanceof RSAPublicKey rsaPublicKey) {
            assertThat(rsaPublicKey.getPublicExponent().toString()).isEqualTo("65537");
            assertThat(rsaPublicKey.getModulus().toString()).isEqualTo(
                    "15716563600099515211328423611954225824916533615230691376921478986333060601" +
                    "57552226919808527872386330392237985485200227383711345332616167018657122816541835915" +
                    "10152175666741773856946951958086291801538991290666845716385778192871160342792145141" +
                    "097809456369701048006656617692262816450908563750970503449532801209031"
            );
        } else {
            fail("PublicKey is not an instance of RSAPublicKey.");
        }
    }

    @Test
    void loadTestRsaKeyMustUseAlgorithmRSAWhenPEM() throws PublicKeyParseException {
        String pem = """
                ---- BEGIN SSH2 PUBLIC KEY ----
                AAAAB3NzaC1yc2EAAAADAQABAAABgQDfz7EED7gE8qbDKg6tUMtL3yW/
                W1kiGzWBbVhOMiFYjnVje8mNF5jNe8rj+tg6fKA9dMpULrTIaFC5Dbhq4c0vCRqTbzrPkrXCURAdWx1lb+NzfTIOqLVeUZlnh
                NPXBPVss+pnkawsI4HUiMRuScQnzrbvY9HcLhb69Qe9sZvSx1R46F9BEOZduX3QJxV2NBer6zJAZxv/YJt/ObzSFSnWN5isG+X
                x/tIUZHighuqH7k0pzanp8IH11O6qXuCyqk9c7110w/vlQ+N8N3UhtAOhPCljKjYJo0KgF9C//l9eVKi/QICGVbr6vzvAyiWg
                uf1lMHUEmsDlGQqfq/nnKCQYlz2V6oOaPt6ggmdmNCppFOSXri0dm1X4TnLASpIFetdjFGTU+sH75SmSrPlYn5HOxcqeVGgHNIZ2
                taY+u12DWtY7cqFXvHE6pFmPme2Z0wQrAq/rH2BqGl3pkWVDIItbtsVsVTlS6E1Y/dXJkjaBiBZTc7aeBLa58oj5F4JYb
                sc=
                ---- END SSH2 PUBLIC KEY ----
                """;
        PublicKey rsaKey = PublicKeyReaderUtil.load(pem, false);
        assertEquals("RSA", rsaKey.getAlgorithm());
    }

    @Test
    void loadTestRsaKeyMustUseAlgorithmRSAWhenPEMWithSimpleHeader() throws PublicKeyParseException {
        String pem = """
                ---- BEGIN SSH2 PUBLIC KEY ----
                header:foo
                AAAAB3NzaC1yc2EAAAADAQABAAABgQDfz7EED7gE8qbDKg6tUMtL3yW/
                W1kiGzWBbVhOMiFYjnVje8mNF5jNe8rj+tg6fKA9dMpULrTIaFC5Dbhq4c0vCRqTbzrPkrXCURAdWx1lb+NzfTIOqLVeUZlnh
                NPXBPVss+pnkawsI4HUiMRuScQnzrbvY9HcLhb69Qe9sZvSx1R46F9BEOZduX3QJxV2NBer6zJAZxv/YJt/ObzSFSnWN5isG+X
                x/tIUZHighuqH7k0pzanp8IH11O6qXuCyqk9c7110w/vlQ+N8N3UhtAOhPCljKjYJo0KgF9C//l9eVKi/QICGVbr6vzvAyiWg
                uf1lMHUEmsDlGQqfq/nnKCQYlz2V6oOaPt6ggmdmNCppFOSXri0dm1X4TnLASpIFetdjFGTU+sH75SmSrPlYn5HOxcqeVGgHNIZ2
                taY+u12DWtY7cqFXvHE6pFmPme2Z0wQrAq/rH2BqGl3pkWVDIItbtsVsVTlS6E1Y/dXJkjaBiBZTc7aeBLa58oj5F4JYb
                sc=
                ---- END SSH2 PUBLIC KEY ----
                """;
        PublicKey rsaKey = PublicKeyReaderUtil.load(pem, false);
        assertEquals("RSA", rsaKey.getAlgorithm());
    }

    @Test
    void loadTestRsaKeyMustUseAlgorithmRSAWhenPEMWithMultilineHeader() throws PublicKeyParseException {
        String pem = """
                ---- BEGIN SSH2 PUBLIC KEY ----
                header:foo \\
                header2:foo2
                AAAAB3NzaC1yc2EAAAADAQABAAABgQDfz7EED7gE8qbDKg6tUMtL3yW/
                W1kiGzWBbVhOMiFYjnVje8mNF5jNe8rj+tg6fKA9dMpULrTIaFC5Dbhq4c0vCRqTbzrPkrXCURAdWx1lb+NzfTIOqLVeUZlnh
                NPXBPVss+pnkawsI4HUiMRuScQnzrbvY9HcLhb69Qe9sZvSx1R46F9BEOZduX3QJxV2NBer6zJAZxv/YJt/ObzSFSnWN5isG+X
                x/tIUZHighuqH7k0pzanp8IH11O6qXuCyqk9c7110w/vlQ+N8N3UhtAOhPCljKjYJo0KgF9C//l9eVKi/QICGVbr6vzvAyiWg
                uf1lMHUEmsDlGQqfq/nnKCQYlz2V6oOaPt6ggmdmNCppFOSXri0dm1X4TnLASpIFetdjFGTU+sH75SmSrPlYn5HOxcqeVGgHNIZ2
                taY+u12DWtY7cqFXvHE6pFmPme2Z0wQrAq/rH2BqGl3pkWVDIItbtsVsVTlS6E1Y/dXJkjaBiBZTc7aeBLa58oj5F4JYb
                sc=
                ---- END SSH2 PUBLIC KEY ----
                """;
        PublicKey rsaKey = PublicKeyReaderUtil.load(pem, false);
        assertEquals("RSA", rsaKey.getAlgorithm());
    }

    @Test
    void loadTestEd25519KeyMustUseAlgorithmEdDSAAndParameterSpecEd25519WhenOpenSSH() throws PublicKeyParseException {
        PublicKey ed25519Key = PublicKeyReaderUtil.load("ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIMQltFutaGpkyuDLScqHRZtknBd4c/IJCkVsY7WFS+gK", false);
        assertThat(ed25519Key.getAlgorithm()).isEqualTo("EdDSA");
        assertThat(ed25519Key).isInstanceOf(EdECPublicKey.class);
        if (ed25519Key instanceof EdECPublicKey edECPublicKey) {
            assertThat(edECPublicKey.getParams().getName()).isEqualTo("Ed25519");
            EdECPoint point = edECPublicKey.getPoint();
            assertThat(point.getY().toString()).isEqualTo("4933558240612590083993219300962144511659480787166253786943024901159224878532");
            assertThat(point.isXOdd()).isFalse();
        } else {
            fail("PublicKey is not an instance of EdECPublicKey.");
        }
    }

    // OpenSSH does not support Ed25519 in PEM format, so no need for such a test case. See https://bugzilla.mindrot.org/show_bug.cgi?id=3195

    @Test
    void loadTestUnknownKeyFormat() {
        PublicKeyParseException publicKeyParseException = assertThrows(PublicKeyParseException.class, () -> PublicKeyReaderUtil.load("foo", false));
        assertThat(publicKeyParseException).hasMessage("Corrupt or unknown public key file format");
    }

    @Test
    void loadTestMalformedTypeByteArray() {
        // echo -n 'foobar'
        PublicKeyParseException publicKeyParseException = assertThrows(PublicKeyParseException.class, () -> PublicKeyReaderUtil.load("ssh-rsa Zm9vYmFy", false));
        assertThat(publicKeyParseException).hasMessage("Public key length is shorter than 2048 bits or byte array is corrupt.");
    }

    @Test
    void loadTestUnknownCertificateFormat() {
        // echo -ne '\x00\x00\x00\x06foobar'
        PublicKeyParseException publicKeyParseException = assertThrows(PublicKeyParseException.class,
                () -> PublicKeyReaderUtil.load("ssh-rsa AAAABmZvb2Jhcg==", false));
        assertThat(publicKeyParseException).hasMessage("Corrupt or unknown public key certificate format");
    }

    @Test
    void loadTestCorruptOpenSSH() {
        PublicKeyParseException publicKeyParseException = assertThrows(PublicKeyParseException.class, () -> PublicKeyReaderUtil.load("ssh-rsa", false));
        assertThat(publicKeyParseException).hasMessage("Corrupt OpenSSH public key string");
    }

    @Test
    void loadTestCorruptPEMEndMissing() {
        PublicKeyParseException publicKeyParseException = assertThrows(PublicKeyParseException.class, () -> PublicKeyReaderUtil.load("-", false));
        assertThat(publicKeyParseException).hasMessage("Corrupt SECSSH public key string");
    }

    @Test
    void loadTestCorruptPEMHeaderInBody() {
        // echo -n 'foobar'
        String pem = """
                ---- BEGIN SSH2 PUBLIC KEY ----
                Zm9vYmFy
                a:b
                ---- END SSH2 PUBLIC KEY ----
                """;
        PublicKeyParseException publicKeyParseException = assertThrows(PublicKeyParseException.class, () -> PublicKeyReaderUtil.load(pem, false));
        assertThat(publicKeyParseException).hasMessage("Corrupt SECSSH public key string");
    }

    @Test
    void loadTestMalformedRSAExponentByteArray() {
        // echo -ne '\x00\x00\x00\x07ssh-rsa\x12\x13\x14\x15'
        PublicKeyParseException publicKeyParseException = assertThrows(PublicKeyParseException.class,
                () -> PublicKeyReaderUtil.load("ssh-rsa AAAAB3NzaC1yc2ESExQV", false));
        assertThat(publicKeyParseException).hasMessage("Public key length is shorter than 2048 bits or byte array is corrupt.");
    }

    @Test
    void loadTestMalformedRSAModulusByteArray() {
        // echo -ne '\x00\x00\x00\x07ssh-rsa\x00\x00\x00\x01\x00\x12\x13\x14\x15'
        PublicKeyParseException publicKeyParseException = assertThrows(PublicKeyParseException.class,
                () -> PublicKeyReaderUtil.load("ssh-rsa AAAAB3NzaC1yc2EAAAABABITFBU=", false));
        assertThat(publicKeyParseException).hasMessage("Public key length is shorter than 2048 bits or byte array is corrupt.");
    }

    @Test
    void loadTestInvalidRSAParameters() {
        // echo -ne '\x00\x00\x00\x07ssh-rsa\x00\x00\x00\x01\x00\x00\x00\x00\x01\x00'
        PublicKeyParseException publicKeyParseException = assertThrows(PublicKeyParseException.class,
                () -> PublicKeyReaderUtil.load("ssh-rsa AAAAB3NzaC1yc2EAAAABAAAAAAEA", false));
        assertThat(publicKeyParseException).hasMessage("SSH2RSA: error decoding public key blob");
    }

    @Test
    void loadTestMalformedEd25519PointByteArray() {
        // echo -ne '\x00\x00\x00\x0Bssh-ed25519\x12\x13\x14\x15'
        PublicKeyParseException publicKeyParseException = assertThrows(PublicKeyParseException.class,
                () -> PublicKeyReaderUtil.load("ssh-ed25519 AAAAC3NzaC1lZDI1NTE5EhMUFQ==", false));
        assertThat(publicKeyParseException).hasMessage("Public key length is shorter than 2048 bits or byte array is corrupt.");
    }

    @Test
    void loadTestInvalidEd25519Parameters() {
        // echo -ne '\x00\x00\x00\x0Bssh-ed25519\x00\x00\x00\x01\x00'
        PublicKeyParseException publicKeyParseException = assertThrows(PublicKeyParseException.class,
                () -> PublicKeyReaderUtil.load("ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAAQA=", false));
        assertThat(publicKeyParseException).hasMessage("SSH2ED25519: byte array for EdECPoint must be 32 bytes long");
    }

    // No way to test SSH2ED25519_ERROR_DECODING_PUBLIC_KEY_BLOB.

    @Test
    void loadTestEd25519ForbiddenInFipsMode() {
        PublicKeyParseException publicKeyParseException = assertThrows(PublicKeyParseException.class,
                () -> PublicKeyReaderUtil.load("ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIMQltFutaGpkyuDLScqHRZtknBd4c/IJCkVsY7WFS+gK", true));
        assertThat(publicKeyParseException).hasMessage("SSH2ED25519: this key type is not allowed when running clusters in FIPS mode");
    }

}