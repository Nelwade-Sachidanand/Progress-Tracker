package com.novillex.progresstracker.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class JwtUtil {

	private static final String SECRET = "my-super-secret-key-my-super-secret-key";

	private static final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

	public static String generateAccessToken(String userId, String username, String role) {

		return Jwts.builder().setSubject(username).claim("userId", userId).claim("role", role).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 5))
				.signWith(key, SignatureAlgorithm.HS256).compact();
	}

	public static String generateRefreshToken(String userId, String username, String role) {

		return Jwts.builder().setSubject(username).claim("userId", userId).claim("role", role).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7))
				.signWith(key, SignatureAlgorithm.HS256).compact();
	}

	public static Claims extractClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
	}
}
