package dev.marlondvg.mc_deck_api.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("app.login-limit")
public record LoginLimitProperties(
		@DefaultValue("5") int maxFailuresPerUsername,
		@DefaultValue("20") int maxFailuresPerIp,
		@DefaultValue("15m") Duration window) {

	public LoginLimitProperties {
		if (maxFailuresPerUsername < 1 || maxFailuresPerIp < 1 || window.isNegative() || window.isZero()) {
			throw new IllegalStateException("app.login-limit values must be positive");
		}
	}
}
