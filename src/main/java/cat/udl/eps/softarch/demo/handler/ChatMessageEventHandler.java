package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.ChatMessage;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.ParticipationStatus;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;

@Component
@RepositoryEventHandler
public class ChatMessageEventHandler {

    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;

    public ChatMessageEventHandler(UserRepository userRepository,
                                   ParticipationRepository participationRepository) {
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
    }

    @HandleBeforeCreate
    public void handleChatMessagePreCreate(ChatMessage message) {
        Meetup meetup = message.getMeetup();
        if (meetup == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meetup is required");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication == null ? null : authentication.getName();
        User sender = username == null ? null : userRepository.findById(username).orElse(null);
        if (sender == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required to chat");
        }

        boolean confirmed = participationRepository.existsByParticipantIdAndMeetupIdAndStatusIn(
            username, meetup.getId(), EnumSet.of(ParticipationStatus.CONFIRMED));
        if (!confirmed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meetup not found");
        }

        message.setSender(sender);
    }
}
