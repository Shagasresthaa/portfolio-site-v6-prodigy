package com.sresthaa.publicui.dto;

import com.sresthaa.publicui.model.ReactionVote;

// A transition, not a target state - server has no visitor identity to look up the prior vote,
// so the client (which tracks it in localStorage) sends both ends and the server applies the delta.
public record BlogReactionRequest(ReactionVote previousVote, ReactionVote vote) {
}
