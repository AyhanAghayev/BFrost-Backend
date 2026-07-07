package com.bfrost.backend.chat;

import java.io.Serializable;
import java.util.UUID;

public record ConversationReadId(UUID userId, UUID conversationId) implements Serializable {}