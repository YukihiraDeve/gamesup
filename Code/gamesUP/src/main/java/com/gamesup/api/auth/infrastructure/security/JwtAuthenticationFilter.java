package com.gamesup.api.auth.infrastructure.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final GamesUpUserDetailsService userDetailsService;
	private final RestAuthenticationEntryPoint authenticationEntryPoint;

	public JwtAuthenticationFilter(
			JwtService jwtService,
			GamesUpUserDetailsService userDetailsService,
			RestAuthenticationEntryPoint authenticationEntryPoint) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.authenticationEntryPoint = authenticationEntryPoint;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			String token = authorization.substring(BEARER_PREFIX.length());
			String username = jwtService.extractUsername(token);
			if (SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);
				if (jwtService.isTokenValid(token, userDetails)) {
					UsernamePasswordAuthenticationToken authentication =
							new UsernamePasswordAuthenticationToken(
									userDetails,
									null,
									userDetails.getAuthorities());
					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			}
		} catch (JwtException | IllegalArgumentException | UsernameNotFoundException exception) {
			SecurityContextHolder.clearContext();
			authenticationEntryPoint.commence(
					request,
					response,
					new InvalidTokenAuthenticationException(exception));
			return;
		}

		filterChain.doFilter(request, response);
	}
}
