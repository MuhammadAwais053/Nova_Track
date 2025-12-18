package com.example.novatrack.utils;

import android.util.Patterns;

public class ValidationHelper {

    public static boolean isValidEmail(String email) {
        return email != null && !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}