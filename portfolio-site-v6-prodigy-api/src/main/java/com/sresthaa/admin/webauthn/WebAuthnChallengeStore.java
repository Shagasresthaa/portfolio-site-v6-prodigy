package com.sresthaa.admin.webauthn;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.webauthn4j.data.client.challenge.Challenge;

// Holds pending WebAuthn challenges in memory, keyed by username - no HTTP session exists
// (stateless JWT API) to hold them instead.
@Component
public class WebAuthnChallengeStore {

	private static final long EXPIRY_MILLIS = 5 * 60 * 1000;

	private record PendingChallenge(Challenge challenge, Instant expiresAt) {
	}

	private final Map<String, PendingChallenge> registrationChallenges = new ConcurrentHashMap<>();
	private final Map<String, PendingChallenge> authenticationChallenges = new ConcurrentHashMap<>();

	public void putRegistrationChallenge(String username, Challenge challenge) {
		registrationChallenges.put(username, new PendingChallenge(challenge, expiry()));
	}

	public Optional<Challenge> consumeRegistrationChallenge(String username) {
		return consume(registrationChallenges, username);
	}

	public void putAuthenticationChallenge(String username, Challenge challenge) {
		authenticationChallenges.put(username, new PendingChallenge(challenge, expiry()));
	}

	public Optional<Challenge> consumeAuthenticationChallenge(String username) {
		return consume(authenticationChallenges, username);
	}

	private Optional<Challenge> consume(Map<String, PendingChallenge> store, String username) {
		PendingChallenge pending = store.remove(username);
		if (pending == null || pending.expiresAt().isBefore(Instant.now())) {
			return Optional.empty();
		}
		return Optional.of(pending.challenge());
	}

	private Instant expiry() {
		return Instant.now().plusMillis(EXPIRY_MILLIS);
	}
}
