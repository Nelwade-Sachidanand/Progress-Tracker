package com.dashboard.filter;

import io.jsonwebtoken.Claims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.dashboard.util.JwtUtil;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getServletPath();

		if (path.equals("/dashboard/login")) {
			filterChain.doFilter(request, response);
			return;
		}

		String header = request.getHeader("Authorization");

		if (header == null || !header.startsWith("Bearer ")) {
			response.setStatus(401);
			response.getWriter().write("Unauthorized access is denied");
			return;
		}

		String token = header.substring(7);

		try {

			Claims claims = JwtUtil.extractClaims(token);

			String username = claims.getSubject();
			String role = (String) claims.get("role");
			request.setAttribute("username", username);
			request.setAttribute("role", role);

			UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null,
					List.of());

			SecurityContextHolder.getContext().setAuthentication(auth);

		} catch (Exception e) {
			response.setStatus(401);
			response.getWriter().write("Invalid Token");
			return;
		}

		filterChain.doFilter(request, response);
	}
}