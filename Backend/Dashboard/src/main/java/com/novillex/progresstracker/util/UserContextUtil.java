package com.novillex.progresstracker.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

public class UserContextUtil {

	public static String getCurrentUser() {

		return SecurityContextHolder.getContext().getAuthentication().getName();
	}

	public static String getCurrentUserId() {

		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

		if (attributes == null) {
			return null;
		}

		HttpServletRequest request = attributes.getRequest();

		return (String) request.getAttribute("userId");
	}

	public static String getCurrentUserRole() {

		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

		if (attributes == null) {
			return null;
		}

		HttpServletRequest request = attributes.getRequest();

		return (String) request.getAttribute("role");
	}
}