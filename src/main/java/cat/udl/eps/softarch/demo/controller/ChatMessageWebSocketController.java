package cat.udl.eps.softarch.demo.controller;

import cat.udl.eps.softarch.demo.domain.ChatMessage;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.domain.enums.ParticipationStatus;
import cat.udl.eps.softarch.demo.repository.ChatMessageRepository;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.EnumSet;

@Controller
public class ChatMessageWebSocketController {

    private final ChatMessageRepository chatMessageRepository;
    private final MeetupRepository meetupRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageWebSocketController(ChatMessageRepository chatMessageRepository,
                                         MeetupRepository meetupRepository,
                                         UserRepository userRepository,
                                         ParticipationRepository participationRepository,
                                         SimpMessagingTemplate messagingTemplate) {
        this.chatMessageRepository = chatMessageRepository;
        this.meetupRepository = meetupRepository;
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/meetups/{meetupId}/chat")
    public void sendMessage(@DestinationVariable Long meetupId, @Payload ChatMessage payload, Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new AccessDeniedException("Unauthorized");
        }
        String username = principal.getName();
        Meetup meetup = meetupRepository.findById(meetupId).orElseThrow(() -> new AccessDeniedException("Not authorized"));
        boolean confirmed = participationRepository.existsByParticipantIdAndMeetupIdAndStatusIn(
                username, meetupId, EnumSet.of(ParticipationStatus.CONFIRMED));
        if (!confirmed) {
            throw new AccessDeniedException("Not authorized");
        }
        User sender = userRepository.findById(username).orElseThrow(() -> new AccessDeniedException("Unauthorized"));
        ChatMessage message = new ChatMessage();
        message.setText(payload.getText());
        message.setMeetup(meetup);
        message.setSender(sender);
        ChatMessage saved = chatMessageRepository.save(message);
        messagingTemplate.convertAndSend("/topic/meetups/" + meetupId + "/chat", saved);
    }
}
