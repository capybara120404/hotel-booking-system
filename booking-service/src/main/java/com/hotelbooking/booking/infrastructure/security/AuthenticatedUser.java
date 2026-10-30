package com.hotelbooking.booking.infrastructure.security;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class AuthenticatedUser implements UserDetails {
	private static final long serialVersionUID = 1L;
	private final Long id;
	private final String username;
	private final String password;
	private final List<GrantedAuthority> authorities;

	public AuthenticatedUser(Long id, String username, String password,
			Collection<? extends GrantedAuthority> authorities) {
		this.id = Objects.requireNonNull(id, "id");
		this.username = Objects.requireNonNull(username, "username");
		this.password = Objects.requireNonNull(password, "password");
		this.authorities = List.copyOf(authorities);
	}

	public Long getId() {
		return id;
	}
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}
	@Override
	public String getPassword() {
		return password;
	}
	@Override
	public String getUsername() {
		return username;
	}
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}
	@Override
	public boolean isAccountNonLocked() {
		return true;
	}
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}
	@Override
	public boolean isEnabled() {
		return true;
	}
}
