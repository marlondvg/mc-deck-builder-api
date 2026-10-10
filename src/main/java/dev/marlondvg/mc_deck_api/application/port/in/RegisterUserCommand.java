package dev.marlondvg.mc_deck_api.application.port.in;

public record RegisterUserCommand(String username, String password) {

	@Override
	public String toString() {
		return "RegisterUserCommand[username=" + username + ", password=***]";
	}
}
