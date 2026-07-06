package com.sresthaa.admin.webauthn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.webauthn4j.data.client.challenge.DefaultChallenge;

class WebAuthnChallengeStoreTest {

	private final WebAuthnChallengeStore store = new WebAuthnChallengeStore();

	@Test
	void registrationChallengeIsConsumedExactlyOnce() {
		var challenge = new DefaultChallenge();
		store.putRegistrationChallenge("someuser", challenge);

		var first = store.consumeRegistrationChallenge("someuser");
		var second = store.consumeRegistrationChallenge("someuser");

		assertTrue(first.isPresent());
		assertEquals(challenge, first.get());
		assertTrue(second.isEmpty());
	}

	@Test
	void authenticationChallengeIsConsumedExactlyOnce() {
		var challenge = new DefaultChallenge();
		store.putAuthenticationChallenge("someuser", challenge);

		var first = store.consumeAuthenticationChallenge("someuser");
		var second = store.consumeAuthenticationChallenge("someuser");

		assertTrue(first.isPresent());
		assertEquals(challenge, first.get());
		assertTrue(second.isEmpty());
	}

	@Test
	void consumingWithNoPendingChallengeReturnsEmpty() {
		assertTrue(store.consumeRegistrationChallenge("nobody").isEmpty());
		assertTrue(store.consumeAuthenticationChallenge("nobody").isEmpty());
	}

	@Test
	void registrationAndAuthenticationChallengesAreIndependent() {
		var registrationChallenge = new DefaultChallenge();
		store.putRegistrationChallenge("someuser", registrationChallenge);

		assertTrue(store.consumeAuthenticationChallenge("someuser").isEmpty());
		assertTrue(store.consumeRegistrationChallenge("someuser").isPresent());
	}
}
