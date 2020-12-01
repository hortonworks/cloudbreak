package com.sequenceiq.environment.environment.validation.validators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.validation.ValidationResult;

class PublicKeyValidatorTest {

    private final PublicKeyValidator underTest = new PublicKeyValidator();

    @Test
    void testPublicKeyValidationWithValidKeyAndCommentIsValid() {
        String validKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDXF9paZtj6ibDgRGtw2jTls1fysxETxG/rbCN9vYlpDw6ROK90rjXnC0qi43kXbs5Z/Ny" +
                "FDDcYW4U1nUmJCIhQqNJv00ukTbS4McfeNYrZF78KNLtbUP57SfKzjcH1Txz9Zr3ILEADqMyNRzgSB2gw5XnfbbLUFQYcF3hT11gjEw99Qi9eCQ6nC" +
                "M37iy9tCIfMyB5CI24LECv+8UMFdYw+X9JQXgkAlcPi1zmVNckD6dDGGC2nY9mK7jw0dqZ2W/Q2HvGVgP7iSxnKIFIylifPMz0jmtzpjPi4czgr34" +
                "d4PFlQsv8LgwWEQMyTJGQmtF3GGa1o/qT07gePY2tl1sAzOLszfglmkBZS+POYi65fxYGepF5C0Rmc3427dLp+HPl8QSAuq0j92/3LGLOKTjn1qC2" +
                "MjBNhbkhFhDKnv0VQslmN/nkgDKUSHfQqfMwo4HXFIIydUWxT+5PxFCaS4axzGQ2HYylxqonnU3P0DJkh/omegI36HyxxlMNlpf/zn3/zrwSFvKN" +
                "CFvbQezJg8jdqq7VEHx4DH6WhYQ02TLsjmcA0EFp1HxTCbJojD/Ixev/Wc5duHotOBiS0CXdJwyzKSQttQS9NGn+/LvUyiD/Z/Rz2r3B0LpQsQu4" +
                "tI/8F5Jq+QiRgS9cQRnuLZuvAwbSNYI+g+zPdhaZq8fvumWarcQ== apalfi@cloudera.com\n" +
                "\n";

        ValidationResult validationResult = underTest.validatePublicKey(validKey);
        assertFalse(validationResult.hasError());
    }

    @Test
    void testPublicKeyValidationWithBackSlashesInCommentIsValid() {
        String validKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDXF9paZtj6ibDgRGtw2jTls1fysxETxG/rbCN9vYlpDw6ROK90rjXnC0qi43kXbs5Z/Ny" +
                "FDDcYW4U1nUmJCIhQqNJv00ukTbS4McfeNYrZF78KNLtbUP57SfKzjcH1Txz9Zr3ILEADqMyNRzgSB2gw5XnfbbLUFQYcF3hT11gjEw99Qi9eCQ6nC" +
                "M37iy9tCIfMyB5CI24LECv+8UMFdYw+X9JQXgkAlcPi1zmVNckD6dDGGC2nY9mK7jw0dqZ2W/Q2HvGVgP7iSxnKIFIylifPMz0jmtzpjPi4czgr34" +
                "d4PFlQsv8LgwWEQMyTJGQmtF3GGa1o/qT07gePY2tl1sAzOLszfglmkBZS+POYi65fxYGepF5C0Rmc3427dLp+HPl8QSAuq0j92/3LGLOKTjn1qC2" +
                "MjBNhbkhFhDKnv0VQslmN/nkgDKUSHfQqfMwo4HXFIIydUWxT+5PxFCaS4axzGQ2HYylxqonnU3P0DJkh/omegI36HyxxlMNlpf/zn3/zrwSFvKN" +
                "CFvbQezJg8jdqq7VEHx4DH6WhYQ02TLsjmcA0EFp1HxTCbJojD/Ixev/Wc5duHotOBiS0CXdJwyzKSQttQS9NGn+/LvUyiD/Z/Rz2r3B0LpQsQu4" +
                "tI/8F5Jq+QiRgS9cQRnuLZuvAwbSNYI+g+zPdhaZq8fvumWarcQ== office01\\\\en10022@PCL15925\n" +
                "\n";

        ValidationResult validationResult = underTest.validatePublicKey(validKey);
        assertFalse(validationResult.hasError());
    }

    @Test
    void testPublicKeyValidationWithNoCommentIsValid() {
        String validKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDXF9paZtj6ibDgRGtw2jTls1fysxETxG/rbCN9vYlpDw6ROK90rjXnC0qi43kXbs5Z/Ny" +
                "FDDcYW4U1nUmJCIhQqNJv00ukTbS4McfeNYrZF78KNLtbUP57SfKzjcH1Txz9Zr3ILEADqMyNRzgSB2gw5XnfbbLUFQYcF3hT11gjEw99Qi9eCQ6nC" +
                "M37iy9tCIfMyB5CI24LECv+8UMFdYw+X9JQXgkAlcPi1zmVNckD6dDGGC2nY9mK7jw0dqZ2W/Q2HvGVgP7iSxnKIFIylifPMz0jmtzpjPi4czgr34" +
                "d4PFlQsv8LgwWEQMyTJGQmtF3GGa1o/qT07gePY2tl1sAzOLszfglmkBZS+POYi65fxYGepF5C0Rmc3427dLp+HPl8QSAuq0j92/3LGLOKTjn1qC2" +
                "MjBNhbkhFhDKnv0VQslmN/nkgDKUSHfQqfMwo4HXFIIydUWxT+5PxFCaS4axzGQ2HYylxqonnU3P0DJkh/omegI36HyxxlMNlpf/zn3/zrwSFvKN" +
                "CFvbQezJg8jdqq7VEHx4DH6WhYQ02TLsjmcA0EFp1HxTCbJojD/Ixev/Wc5duHotOBiS0CXdJwyzKSQttQS9NGn+/LvUyiD/Z/Rz2r3B0LpQsQu4" +
                "tI/8F5Jq+QiRgS9cQRnuLZuvAwbSNYI+g+zPdhaZq8fvumWarcQ==";

        ValidationResult validationResult = underTest.validatePublicKey(validKey);
        assertFalse(validationResult.hasError());
    }

    @Test
    void testPublicKeyValidationWith45PartsIsValid() {
        String validKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDXF9paZtj6ibDgRGtw2jTls1fysxETxG/rbCN9vYlpDw6ROK90rjXnC0qi43kXbs5Z/Ny" +
                "FDDcYW4U1nUmJCIhQqNJv00ukTbS4McfeNYrZF78KNLtbUP57SfKzjcH1Txz9Zr3ILEADqMyNRzgSB2gw5XnfbbLUFQYcF3hT11gjEw99Qi9eCQ6nC" +
                "M37iy9tCIfMyB5CI24LECv+8UMFdYw+X9JQXgkAlcPi1zmVNckD6dDGGC2nY9mK7jw0dqZ2W/Q2HvGVgP7iSxnKIFIylifPMz0jmtzpjPi4czgr34" +
                "d4PFlQsv8LgwWEQMyTJGQmtF3GGa1o/qT07gePY2tl1sAzOLszfglmkBZS+POYi65fxYGepF5C0Rmc3427dLp+HPl8QSAuq0j92/3LGLOKTjn1qC2" +
                "MjBNhbkhFhDKnv0VQslmN/nkgDKUSHfQqfMwo4HXFIIydUWxT+5PxFCaS4axzGQ2HYylxqonnU3P0DJkh/omegI36HyxxlMNlpf/zn3/zrwSFvKN" +
                "CFvbQezJg8jdqq7VEHx4DH6WhYQ02TLsjmcA0EFp1HxTCbJojD/Ixev/Wc5duHotOBiS0CXdJwyzKSQttQS9NGn+/LvUyiD/Z/Rz2r3B0LpQsQu4" +
                "tI/8F5Jq+QiRgS9cQRnuLZuvAwbSNYI+g+zPdhaZq8fvumWarcQ== Comment@comment.comment 4thpart 5thpartlool";

        ValidationResult validationResult = underTest.validatePublicKey(validKey);
        assertFalse(validationResult.hasError());
    }

    @Test
    void testPublicKeyValidationWithValidNistp256() {
        String validKey = "ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTF9paZtj6ibDgRGtw2jTls1fysxETxG/rbCN9vYlpDw6ROK90rjXnC0qi43kXbs5Z/Ny" +
                "FDDcYW4U1nUmJCIhQqNJv00ukTbS4McfeNYrZF78KNLtbUP57SfKzjcH1Txz9Zr3ILEADqMyNRzgSB2gw5XnfbbLUFQYcF3hT11gjEw99Qi9eCQ6nC" +
                "M37iy9tCIfMyB5CI24LECv+8UMFdYw+X9JQXgkAlcPi1zmVNckD6dDGGC2nY9mK7jw0dqZ2W/Q2HvGVgP7iSxnKIFIylifPMz0jmtzpjPi4czgr34" +
                "d4PFlQsv8LgwWEQMyTJGQmtF3GGa1o/qT07gePY2tl1sAzOLszfglmkBZS+POYi65fxYGepF5C0Rmc3427dLp+HPl8QSAuq0j92/3LGLOKTjn1qC2" +
                "MjBNhbkhFhDKnv0VQslmN/nkgDKUSHfQqfMwo4HXFIIydUWxT+5PxFCaS4axzGQ2HYylxqonnU3P0DJkh/omegI36HyxxlMNlpf/zn3/zrwSFvKN" +
                "CFvbQezJg8jdqq7VEHx4DH6WhYQ02TLsjmcA0EFp1HxTCbJojD/Ixev/Wc5duHotOBiS0CXdJwyzKSQttQS9NGn+/LvUyiD/Z/Rz2r3B0LpQsQu4" +
                "tI/8F5Jq+QiRgS9cQRnuLZuvAwbSNYI+g+zPdhaZq8fvumWarcQ== apalfi@cloudera.com\n" +
                "\n";

        ValidationResult validationResult = underTest.validatePublicKey(validKey);
        assertFalse(validationResult.hasError());
    }

    @Test
    void testPublicKeyValidationWithValidNistp384() {
        String validKey = "ecdsa-sha2-nistp384 AAAAE2VjZHNhLXNoYTItbmlzdHAzODQAAAAIbmlzdHAzODOK90rjXnC0qi43kXbs5Z/Ny" +
                "FDDcYW4U1nUmJCIhQqNJv00ukTbS4McfeNYrZF78KNLtbUP57SfKzjcH1Txz9Zr3ILEADqMyNRzgSB2gw5XnfbbLUFQYcF3hT11gjEw99Qi9eCQ6nC" +
                "M37iy9tCIfMyB5CI24LECv+8UMFdYw+X9JQXgkAlcPi1zmVNckD6dDGGC2nY9mK7jw0dqZ2W/Q2HvGVgP7iSxnKIFIylifPMz0jmtzpjPi4czgr34" +
                "d4PFlQsv8LgwWEQMyTJGQmtF3GGa1o/qT07gePY2tl1sAzOLszfglmkBZS+POYi65fxYGepF5C0Rmc3427dLp+HPl8QSAuq0j92/3LGLOKTjn1qC2" +
                "MjBNhbkhFhDKnv0VQslmN/nkgDKUSHfQqfMwo4HXFIIydUWxT+5PxFCaS4axzGQ2HYylxqonnU3P0DJkh/omegI36HyxxlMNlpf/zn3/zrwSFvKN" +
                "CFvbQezJg8jdqq7VEHx4DH6WhYQ02TLsjmcA0EFp1HxTCbJojD/Ixev/Wc5duHotOBiS0CXdJwyzKSQttQS9NGn+/LvUyiD/Z/Rz2r3B0LpQsQu4" +
                "tI/8F5Jq+QiRgS9cQRnuLZuvAwbSNYI+g+zPdhaZq8fvumWarcQ== apalfi@cloudera.com\n" +
                "\n";

        ValidationResult validationResult = underTest.validatePublicKey(validKey);
        assertFalse(validationResult.hasError());
    }

    @Test
    void testPublicKeyValidationWithValidNistp521() {
        String validKey = "ecdsa-sha2-nistp521 AAAAE2VjZHNhLXNoYTItbmlzdHA1MjEAAAAIbmlzdHA1Mj" +
                "FDDcYW4U1nUmJCIhQqNJv00ukTbS4McfeNYrZF78KNLtbUP57SfKzjcH1Txz9Zr3ILEADqMyNRzgSB2gw5XnfbbLUFQYcF3hT11gjEw99Qi9eCQ6nC" +
                "M37iy9tCIfMyB5CI24LECv+8UMFdYw+X9JQXgkAlcPi1zmVNckD6dDGGC2nY9mK7jw0dqZ2W/Q2HvGVgP7iSxnKIFIylifPMz0jmtzpjPi4czgr34" +
                "d4PFlQsv8LgwWEQMyTJGQmtF3GGa1o/qT07gePY2tl1sAzOLszfglmkBZS+POYi65fxYGepF5C0Rmc3427dLp+HPl8QSAuq0j92/3LGLOKTjn1qC2" +
                "MjBNhbkhFhDKnv0VQslmN/nkgDKUSHfQqfMwo4HXFIIydUWxT+5PxFCaS4axzGQ2HYylxqonnU3P0DJkh/omegI36HyxxlMNlpf/zn3/zrwSFvKN" +
                "CFvbQezJg8jdqq7VEHx4DH6WhYQ02TLsjmcA0EFp1HxTCbJojD/Ixev/Wc5duHotOBiS0CXdJwyzKSQttQS9NGn+/LvUyiD/Z/Rz2r3B0LpQsQu4" +
                "tI/8F5Jq+QiRgS9cQRnuLZuvAwbSNYI+g+zPdhaZq8fvumWarcQ== apalfi@cloudera.com\n" +
                "\n";

        ValidationResult validationResult = underTest.validatePublicKey(validKey);
        assertFalse(validationResult.hasError());
    }

    @Test
    void testPublicKeyValidationWithValidSshEd25519() {
        String validKey = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5NoYTItbmlzdHA1MjEAAAAIbmlzdHA1Mj" +
                "FDDcYW4U1nUmJCIhQqNJv00ukTbS4McfeNYrZF78KNLtbUP57SfKzjcH1Txz9Zr3ILEADqMyNRzgSB2gw5XnfbbLUFQYcF3hT11gjEw99Qi9eCQ6nC" +
                "M37iy9tCIfMyB5CI24LECv+8UMFdYw+X9JQXgkAlcPi1zmVNckD6dDGGC2nY9mK7jw0dqZ2W/Q2HvGVgP7iSxnKIFIylifPMz0jmtzpjPi4czgr34" +
                "d4PFlQsv8LgwWEQMyTJGQmtF3GGa1o/qT07gePY2tl1sAzOLszfglmkBZS+POYi65fxYGepF5C0Rmc3427dLp+HPl8QSAuq0j92/3LGLOKTjn1qC2" +
                "MjBNhbkhFhDKnv0VQslmN/nkgDKUSHfQqfMwo4HXFIIydUWxT+5PxFCaS4axzGQ2HYylxqonnU3P0DJkh/omegI36HyxxlMNlpf/zn3/zrwSFvKN" +
                "CFvbQezJg8jdqq7VEHx4DH6WhYQ02TLsjmcA0EFp1HxTCbJojD/Ixev/Wc5duHotOBiS0CXdJwyzKSQttQS9NGn+/LvUyiD/Z/Rz2r3B0LpQsQu4" +
                "tI/8F5Jq+QiRgS9cQRnuLZuvAwbSNYI+g+zPdhaZq8fvumWarcQ== apalfi@cloudera.com\n" +
                "\n";

        ValidationResult validationResult = underTest.validatePublicKey(validKey);
        assertFalse(validationResult.hasError());
    }

    @Test
    void testPublicKeyValidationWithValidSshDss() {
        String validKey = "ssh-dss AAAAB3NzaC1kc3AAAAC3NzaC1lZDI1NTE5NoYTItbmlzdHA1MjEAAAAIbmlzdHA1Mj" +
                "FDDcYW4U1nUmJCIhQqNJv00ukTbS4McfeNYrZF78KNLtbUP57SfKzjcH1Txz9Zr3ILEADqMyNRzgSB2gw5XnfbbLUFQYcF3hT11gjEw99Qi9eCQ6nC" +
                "M37iy9tCIfMyB5CI24LECv+8UMFdYw+X9JQXgkAlcPi1zmVNckD6dDGGC2nY9mK7jw0dqZ2W/Q2HvGVgP7iSxnKIFIylifPMz0jmtzpjPi4czgr34" +
                "d4PFlQsv8LgwWEQMyTJGQmtF3GGa1o/qT07gePY2tl1sAzOLszfglmkBZS+POYi65fxYGepF5C0Rmc3427dLp+HPl8QSAuq0j92/3LGLOKTjn1qC2" +
                "MjBNhbkhFhDKnv0VQslmN/nkgDKUSHfQqfMwo4HXFIIydUWxT+5PxFCaS4axzGQ2HYylxqonnU3P0DJkh/omegI36HyxxlMNlpf/zn3/zrwSFvKN" +
                "CFvbQezJg8jdqq7VEHx4DH6WhYQ02TLsjmcA0EFp1HxTCbJojD/Ixev/Wc5duHotOBiS0CXdJwyzKSQttQS9NGn+/LvUyiD/Z/Rz2r3B0LpQsQu4" +
                "tI/8F5Jq+QiRgS9cQRnuLZuvAwbSNYI+g+zPdhaZq8fvumWarcQ== apalfi@cloudera.com\n" +
                "\n";

        ValidationResult validationResult = underTest.validatePublicKey(validKey);
        assertFalse(validationResult.hasError());
    }

    @Test
    void testPublicKeyValidationWithInvalidAlgorithm() {
        String validKey = "invalid-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDXF9paZtj6ibDgRGtw2jTls1fysxETxG/rbCN9vYlpDw6ROK90rjXnC0qi43kXbs5Z/Ny" +
                "FDDcYW4U1nUmJCIhQqNJv00ukTbS4McfeNYrZF78KNLtbUP57SfKzjcH1Txz9Zr3ILEADqMyNRzgSB2gw5XnfbbLUFQYcF3hT11gjEw99Qi9eCQ6nC" +
                "M37iy9tCIfMyB5CI24LECv+8UMFdYw+X9JQXgkAlcPi1zmVNckD6dDGGC2nY9mK7jw0dqZ2W/Q2HvGVgP7iSxnKIFIylifPMz0jmtzpjPi4czgr34" +
                "d4PFlQsv8LgwWEQMyTJGQmtF3GGa1o/qT07gePY2tl1sAzOLszfglmkBZS+POYi65fxYGepF5C0Rmc3427dLp+HPl8QSAuq0j92/3LGLOKTjn1qC2" +
                "MjBNhbkhFhDKnv0VQslmN/nkgDKUSHfQqfMwo4HXFIIydUWxT+5PxFCaS4axzGQ2HYylxqonnU3P0DJkh/omegI36HyxxlMNlpf/zn3/zrwSFvKN" +
                "CFvbQezJg8jdqq7VEHx4DH6WhYQ02TLsjmcA0EFp1HxTCbJojD/Ixev/Wc5duHotOBiS0CXdJwyzKSQttQS9NGn+/LvUyiD/Z/Rz2r3B0LpQsQu4" +
                "tI/8F5Jq+QiRgS9cQRnuLZuvAwbSNYI+g+zPdhaZq8fvumWarcQ== apalfi@hortonworks.com\n" +
                "\n";

        ValidationResult validationResult = underTest.validatePublicKey(validKey);
        assertTrue(validationResult.hasError());
    }

    @Test
    void testPublicKeyValidationWithInvalidKey() {
        String validKey = "ecdsa-sha2-nistp521 AAAAE2VjZHNhLXNoYTItbmlzdHAdAACAQDXF9paZtj6ibDgRGtYlpDw6ROK90rjXnC0qi43kXbs5Z/Ny" +
                "FDDcYW4U1nUmJCIhQqNJv00ukTbS4McfeNYrZF78KNLtbUP57SfKzjcH1Txz9Zr3ILEADqMyNRzgSB2gw5XnfbbLUFQYcF3hT11gjEw99Qi9eCQ6nC" +
                "M37iy9tCIfMyB5CI24LECv+8UMFdYw+X9JQXgkAlcPi1zmVNckD6dDGGC2nY9mK7jw0dqZ2W/Q2HvGVgP7iSxnKIFIylifPMz0jmtzpjPi4czgr34" +
                "d4PFlQsv8LgwWEQMyTJGQmtF3GGa1o/qT07gePY2tl1sAzOLszfglmkBZS+POYi65fxYGepF5C0Rmc3427dLp+HPl8QSAuq0j92/3LGLOKTjn1qC2" +
                "MjBNhbkhFhDKnv0VQslmN/nkgDKUSHfQqfMwo4HXFIIydUWxT+5PxFCaS4axzGQ2HYylxqonnU3P0DJkh/omegI36HyxxlMNlpf/zn3/zrwSFvKN" +
                "CFvbQezJg8jdqq7VEHx4DH6WhYQ02TLsjmcA0EFp1HxTCbJojD/Ixev/Wc5duHotOBiS0CXdJwyzKSQttQS9NGn+/LvUyiD/Z/Rz2r3B0LpQsQu4" +
                "tI/8F5Jq+QiRgS9cQRnuLZuvAwbSNYI+g+zPdhaZq8fvumWarcQ== apalfi@hortonworks.com\n" +
                "\n";

        ValidationResult validationResult = underTest.validatePublicKey(validKey);
        assertTrue(validationResult.hasError());
    }
}