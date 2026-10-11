package dev.marlondvg.mc_deck_api.application.port.in;

public record LoginCommand(String username, String password, String clientIp) {

	@Override
	public String toString() {
		return "LoginCommand[username=" + username + ", password=***, clientIp=" + clientIp + "]";
	}
}
