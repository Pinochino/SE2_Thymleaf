package com.example.SE2.utils;

import jakarta.servlet.http.HttpServletRequest;

public class AuthUtil {

    public static String getSiteUrl(HttpServletRequest request) {
        String siteUrl = request.getRequestURL().toString();
        return siteUrl.replace(request.getServletPath(), "");
    }
}
