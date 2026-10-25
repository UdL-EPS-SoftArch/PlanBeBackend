package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.ParticipationStatus;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Pattern MEETUP_TOPIC_PATTERN = Pattern.compile(".*/meetups/(\\d+)/chat");
    private static final Pattern MEETUP_APP_PATTERN = Pattern.compile(".*/meetups/(\\d+)/chat");

    private final ParticipationRepository participationRepository;

    public WebSocketAuthInterceptor(ParticipationRepository participationRepository) {
        this.participationRepository = participationRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        Principal principal = accessor.getUser();
        if (principal == null && accessor.getSessionAttributes() != null) {
            Object auth = accessor.getSessionAttributes().get("SPRING_SECURITY_CONTEXT");
            if (auth != null) {
                // Best effort; tests may use STOMP with authenticated Principal
            }
        }

        if (principal == null) {
            // Allow CONNECT without auth here, let controller/security decide; tests use authenticated context
            if (command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
                throw new org.springframework.security.access.AccessDeniedException("Unauthorized");
            }
            return message;
        }

        String username = principal.getName();
        if (username == null) {
            if (command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
                throw new org.springframework.security.access.AccessDeniedException("Unauthorized");
            }
            return message;
        }

        if (command == StompCommand.SUBSCRIBE) {
            String destination = accessor.getDestination();
            Long meetupId = extractMeetupId(destination);
            if (meetupId != null) {
                boolean confirmed = participationRepository.existsByParticipantIdAndMeetupIdAndStatusIn(
                        username, meetupId, EnumSet.of(ParticipationStatus.CONFIRMED));
                if (!confirmed) {
                    throw new org.springframework.security.access.AccessDeniedException("Not authorized");
                }
            }
        } else if (command == StompCommand.SEND) {
            String destination = accessor.getDestination();
            Long meetupId = extractMeetupId(destination);
            if (meetupId != null) {
                boolean confirmed = participationRepository.existsByParticipantIdAndMeetupIdAndStatusIn(
                        username, meetupId, EnumSet.of(ParticipationStatus.CONFIRMED));
                if (!confirmed) {
                    throw new org.springframework.security.access.AccessDeniedException("Not authorized");
                }
            }
        }

        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken) {
            // ok
        }

        return message;
    }

    private Long extractMeetupId(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher m = MEETUP_TOPIC_PATTERN.matcher(destination);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        m = MEETUP_APP_PATTERN.matcher(destination);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
