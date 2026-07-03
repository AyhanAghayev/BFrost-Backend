package com.bfrost.backend.post.dto;

import com.bfrost.backend.post.PollOption;

import java.util.UUID;

public record PollOptionDto(UUID id, String optionText, int voteCount, int position, boolean votedByCurrentUser) {
    public static PollOptionDto from(PollOption o, boolean voted) {
        return new PollOptionDto(o.getId(), o.getOptionText(), o.getVoteCount(), o.getPosition(), voted);
    }
}
