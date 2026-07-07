package com.bfrost.backend.chat;

import java.io.Serializable;
import java.util.UUID;

public record ConversationClearanceId(UUID userId, UUID conversationId) implements Serializable { }
