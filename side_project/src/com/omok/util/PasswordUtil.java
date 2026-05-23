package com.omok.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtil {

    // Prevents instantiation - all methods are static
    private PasswordUtil() {}

    // Converts a plain-text password to a SHA-256 hash string
    // Example: "1234" → "03ac674216f3e15c761ee1a5e255f067..."
    // Only the hash is stored in DB - the original password is never saved
    public static String hash(String password) {
        try {
            // Create MessageDigest instance with SHA-256 algorithm
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Convert string to byte array and compute hash
            byte[] hashBytes = digest.digest(password.getBytes());

            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                // Convert each byte to 2-digit hex (pad with "0" if needed)
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is Java standard - this should never be reached
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    // Compares input password with stored hash during login
    // Hashes the input and checks if it matches the stored hash
    public static boolean verify(String rawPassword, String hashedPassword) {
        return hash(rawPassword).equals(hashedPassword);
    }
}