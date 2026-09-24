package com.alyshapursley.punctuowlity;

import android.util.Base64;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    private PasswordHasher() {
    }

    static String hash(String password) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] derivedKey = derive(password, salt, ITERATIONS);
        return ITERATIONS + ":" + encode(salt) + ":" + encode(derivedKey);
    }

    static boolean verify(String password, String storedValue) {
        if (storedValue == null) {
            return false;
        }

        String[] parts = storedValue.split(":");
        if (parts.length != 3) {
            // Existing development databases may contain the original plain-text format.
            return storedValue.equals(password);
        }

        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = decode(parts[1]);
            byte[] expected = decode(parts[2]);
            byte[] actual = derive(password, salt, iterations);
            return constantTimeEquals(expected, actual);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    static boolean isLegacyValue(String storedValue) {
        return storedValue != null && storedValue.split(":").length != 3;
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable.", exception);
        } finally {
            spec.clearPassword();
        }
    }

    private static String encode(byte[] value) {
        return Base64.encodeToString(value, Base64.NO_WRAP);
    }

    private static byte[] decode(String value) {
        return Base64.decode(value, Base64.NO_WRAP);
    }

    private static boolean constantTimeEquals(byte[] first, byte[] second) {
        if (first.length != second.length) {
            return false;
        }

        int difference = 0;
        for (int index = 0; index < first.length; index++) {
            difference |= first[index] ^ second[index];
        }
        return difference == 0;
    }
}
