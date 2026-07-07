package com.bfrost.backend.chat;

import com.bfrost.backend.chat.dto.MessageDto;
import com.bfrost.backend.chat.dto.SendMessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(SendMessageRequest request, Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());
        MessageDto msg = chatService.sendMessage(request.conversationId(), senderId, request.body());
        UUID recipientId = chatService.getOtherParticipantId(request.conversationId(), senderId);

        messagingTemplate.convertAndSendToUser(recipientId.toString(), "/queue/messages", msg);
        messagingTemplate.convertAndSendToUser(senderId.toString(), "/queue/messages", msg);
    }
}
