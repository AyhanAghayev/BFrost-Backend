package com.bfrost.backend.chat;

import com.bfrost.backend.auth.BFrostUserDetails;
import com.bfrost.backend.chat.dto.ConversationDto;
import com.bfrost.backend.chat.dto.MessageDto;
import com.bfrost.backend.chat.dto.SendMessageRequest;
import com.bfrost.backend.common.CursorPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public List<ConversationDto> list(@AuthenticationPrincipal BFrostUserDetails principal) {
        return chatService.listConversations(principal.userId());
    }

    @GetMapping("/unread-count")
    public long unreadCount(@AuthenticationPrincipal BFrostUserDetails principal) {
        return chatService.getUnreadCount(principal.userId());
    }

    @PostMapping("/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID conversationId,
                         @AuthenticationPrincipal BFrostUserDetails principal) {
        chatService.markRead(conversationId, principal.userId());
    }

    @PostMapping("/with/{otherUserId}")
    public ConversationDto getOrCreate(@PathVariable UUID otherUserId,
                                       @AuthenticationPrincipal BFrostUserDetails principal) {
        return chatService.getOrCreateConversation(principal.userId(), otherUserId);
    }

    @GetMapping("/{conversationId}/messages")
    public CursorPage<MessageDto> messages(@PathVariable UUID conversationId,
                                           @RequestParam(required = false) String cursor,
                                           @RequestParam(defaultValue = "30") int size,
                                           @AuthenticationPrincipal BFrostUserDetails principal) {
        return chatService.getMessages(conversationId, principal.userId(), cursor, size);
    }

    @PostMapping("/{conversationId}/messages")
    public MessageDto send(@PathVariable UUID conversationId,
                           @Valid @RequestBody SendMessageRequest req,
                           @AuthenticationPrincipal BFrostUserDetails principal) {
        return chatService.sendMessage(conversationId, principal.userId(), req.body());
    }

    @PostMapping("/{conversationId}/clear")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@PathVariable UUID conversationId,
                      @AuthenticationPrincipal BFrostUserDetails principal) {
        chatService.clearConversation(conversationId, principal.userId());
    }
}
