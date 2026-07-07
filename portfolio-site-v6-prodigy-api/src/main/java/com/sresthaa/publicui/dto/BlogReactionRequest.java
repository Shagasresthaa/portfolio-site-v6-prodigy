package com.sresthaa.publicui.dto;

import com.sresthaa.publicui.model.ReactionVote;

// previousVote/vote is a transition, not just a target state - the server holds no per-visitor
// identity (see memory: no-user-accounts-by-design), so it can't know what a given browser voted
// last time on its own. The browser is the only side that remembers (localStorage), so it sends
// both ends of the change and the server just applies the delta.
public record BlogReactionRequest(ReactionVote previousVote, ReactionVote vote) {
}
