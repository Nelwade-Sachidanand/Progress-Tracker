package com.dashboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.dashboard.filter.JwtAuthFilter;



@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		System.out.println(http);
		http.csrf(csrf -> csrf.disable());

		http.authorizeHttpRequests(auth -> auth.requestMatchers("/user/login/**").permitAll().anyRequest().authenticated());
		http.addFilterBefore(new JwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
