package com.novillex.progresstracker.filter;

import io.jsonwebtoken.Claims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.novillex.progresstracker.entity.User;
import com.novillex.progresstracker.repository.UserRepository;
import com.novillex.progresstracker.util.JwtUtil;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
	private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

	private final UserRepository userRepository;

	public JwtAuthFilter(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getServletPath();
		if (path.equals("/user/login") || path.equals("/user/refresh") || path.equals("/user/forgotPassword")) {
			filterChain.doFilter(request, response);
			return;
		}

		String header = request.getHeader("Authorization");
		if (header == null || !header.startsWith("Bearer ")) {
			logger.warn("Authorization header missing for URI: {}", request.getRequestURI());

			response.setStatus(401);
			response.getWriter().write("Unauthorized access is denied");
			return;
		}

		String token = header.substring(7);
		try {

			Claims claims = JwtUtil.extractClaims(token);
			String username = claims.getSubject();
			String role = (String) claims.get("role");
			String userId = (String) claims.get("userId");

			User user = userRepository.findByUsername(username).orElse(null);

			if (user != null && Boolean.TRUE.equals(user.getForcePasswordChange())) {

				String uri = request.getServletPath();

				if (!uri.equals("/user/changeTemporaryPassword") && !uri.equals("/user/refresh")) {

					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					response.setContentType("application/json");
					response.getWriter()
							.write("{\"message\":\"Password change is required before accessing the application.\"}");
					return;
				}
			}

			request.setAttribute("username", username);
			request.setAttribute("role", role);
			request.setAttribute("userId", userId);

			List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

			UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null,
					authorities);

			SecurityContextHolder.getContext().setAuthentication(auth);

			logger.info("User authenticated successfully. Username: {}, Role: {}", username, role);

		} catch (Exception e) {
			logger.warn("Invalid JWT token received for URI: {}", request.getRequestURI());
			response.setStatus(401);
			response.getWriter().write("Invalid Token");
			return;
		}

		filterChain.doFilter(request, response);
	}
}