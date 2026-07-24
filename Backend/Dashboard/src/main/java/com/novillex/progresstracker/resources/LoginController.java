package com.novillex.progresstracker.resources;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.StatusCode;
import com.novillex.progresstracker.model.LoginModel;
import com.novillex.progresstracker.model.LoginResponseModel;
import com.novillex.progresstracker.service.UserService;
import com.novillex.progresstracker.util.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/user")
public class LoginController {

	private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

	private final UserService userService;

	public LoginController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping("/login")
	public Response login(@RequestBody LoginModel loginModel, HttpServletResponse response) {

		logger.info("Login request received. Username: {}", loginModel.getUsername());

		Response loginResponse = userService.login(loginModel);

		if (loginResponse.getDetails() != null) {
			LoginResponseModel details = (LoginResponseModel) loginResponse.getDetails();

			ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", details.getRefreshToken()).httpOnly(true)
					.secure(false) // true in production (HTTPS)
					.path("/").sameSite("Lax").maxAge(Duration.ofDays(7)).build();

			response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

			// Don't expose refresh token in response body
			details.setRefreshToken(null);
		}

		return loginResponse;
	}

	@PostMapping("/refresh")
	public Response refreshToken(@CookieValue(value = "refreshToken", required = false) String refreshToken) {

		logger.info("Refreshing token");

		if (refreshToken == null || refreshToken.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found");
		}

		Claims claims = JwtUtil.extractClaims(refreshToken);

		String username = claims.getSubject();
		String role = (String) claims.get("role");
		String userId = (String) claims.get("userId");

		String newAccessToken = JwtUtil.generateAccessToken(userId, username, role);

		return new Response(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE, "Token refreshed successfully",
				newAccessToken);
	}

	@PostMapping("/logout")
	public Response logout(HttpServletResponse response) {

		logger.info("Logout request received");

		ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "").httpOnly(true).secure(false) // true in
																											// production
																											// (HTTPS)
				.path("/").sameSite("Lax").maxAge(Duration.ZERO).build();

		response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

		return new Response(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE, "Logged out successfully", null);
	}
}