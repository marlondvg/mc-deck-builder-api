package dev.marlondvg.mc_deck_api.application.port.out;

public interface PasswordHasher {

	String hash(String rawPassword);

	boolean matches(String rawPassword, String passwordHash);
}
