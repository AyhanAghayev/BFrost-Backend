package com.bfrost.backend.chat;

import com.bfrost.backend.chat.dto.ConversationDto;
import com.bfrost.backend.chat.dto.MessageDto;
import com.bfrost.backend.common.CursorPage;
import com.bfrost.backend.common.exception.ForbiddenException;
import com.bfrost.backend.common.exception.ResourceNotFoundException;
import com.bfrost.backend.user.User;
import com.bfrost.backend.user.UserRepository;
import com.bfrost.backend.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationClearanceRepository clearanceRepository;
    private final ConversationReadRepository readRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public ConversationDto getOrCreateConversation(UUID currentUserId, UUID otherUserId) {
        if (!userService.areFriends(currentUserId, otherUserId)) {
            throw new ForbiddenException("Must be mutual followers (friends) to start a conversation");
        }
        Conversation conv = conversationRepository.findBetween(currentUserId, otherUserId)
                .orElseGet(() -> {
                    boolean currentFirst = unsignedLess(currentUserId, otherUserId);
                    UUID a = currentFirst ? currentUserId : otherUserId;
                    UUID b = currentFirst ? otherUserId : currentUserId;
                    return conversationRepository.save(Conversation.builder()
                            .userA(userRepository.getReferenceById(a))
                            .userB(userRepository.getReferenceById(b))
                            .build());
                });
        return ConversationDto.from(conv, currentUserId,
                messageRepository.countUnreadInConversation(conv.getId(), currentUserId));
    }

    private static boolean unsignedLess(UUID x, UUID y) {
        int hi = Long.compareUnsigned(x.getMostSignificantBits(), y.getMostSignificantBits());
        if (hi != 0) return hi < 0;
        return Long.compareUnsigned(x.getLeastSignificantBits(), y.getLeastSignificantBits()) < 0;
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> listConversations(UUID userId) {
        return conversationRepository.findAllForUser(userId).stream()
                .map(c -> ConversationDto.from(c, userId,
                        messageRepository.countUnreadInConversation(c.getId(), userId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return messageRepository.countUnreadForUser(userId);
    }

    @Transactional
    public void markRead(UUID conversationId, UUID userId) {
        conversationRepository.findById(conversationId)
                .map(c -> { requireParticipant(c, userId); return c; })
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        readRepository.upsertRead(userId, conversationId);
    }

    @Transactional
    public MessageDto sendMessage(UUID conversationId, UUID senderId, String body) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        requireParticipant(conv, senderId);
        User sender = userRepository.getReferenceById(senderId);
        Message msg = Message.builder().conversation(conv).sender(sender).body(body).build();
        messageRepository.save(msg);
        return MessageDto.from(msg);
    }

    @Transactional(readOnly = true)
    public CursorPage<MessageDto> getMessages(UUID conversationId, UUID userId, String cursorStr, int size) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        requireParticipant(conv, userId);
        var cursor = parseCursor(cursorStr);
        List<Message> msgs = messageRepository.findVisibleMessages(conversationId, userId, cursor.createdAt(), cursor.id(), size + 1);
        return CursorPage.of(msgs.stream().map(MessageDto::from).toList(), size + 1,
                dto -> encodeCursor(dto.createdAt(), dto.id()));
    }

    @Transactional(readOnly = true)
    public UUID getOtherParticipantId(UUID conversationId, UUID currentUserId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        requireParticipant(conv, currentUserId);
        return conv.getUserA().getId().equals(currentUserId)
                ? conv.getUserB().getId()
                : conv.getUserA().getId();
    }

    @Transactional
    public void clearConversation(UUID conversationId, UUID userId) {
        conversationRepository.findById(conversationId)
                .map(c -> { requireParticipant(c, userId); return c; })
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        clearanceRepository.upsertClearance(userId, conversationId);
    }

    private void requireParticipant(Conversation conv, UUID userId) {
        if (!conv.getUserA().getId().equals(userId) && !conv.getUserB().getId().equals(userId)) {
            throw new ForbiddenException("Not a participant in this conversation");
        }
    }

    private record Cursor(Instant createdAt, UUID id) {}

    private Cursor parseCursor(String cursorStr) {
        if (cursorStr == null) return new Cursor(null, null);
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursorStr));
            String[] parts = decoded.split("\\|");
            return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (Exception e) {
            return new Cursor(null, null);
        }
    }

    private String encodeCursor(Instant createdAt, UUID id) {
        return Base64.getUrlEncoder().encodeToString((createdAt.toString() + "|" + id).getBytes());
    }
}
