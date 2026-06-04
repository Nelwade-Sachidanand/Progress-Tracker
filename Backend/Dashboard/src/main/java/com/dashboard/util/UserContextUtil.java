package com.dashboard.util;

import org.springframework.security.core.context.SecurityContextHolder;

public class UserContextUtil {

    public static String getCurrentUser() {

        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }
}