package cat.udl.eps.softarch.demo.config;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.domain.enums.Category;
import cat.udl.eps.softarch.demo.domain.enums.InvitationStatus;
import cat.udl.eps.softarch.demo.domain.enums.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.enums.MembershipStatus;
import cat.udl.eps.softarch.demo.domain.enums.NotificationType;
import cat.udl.eps.softarch.demo.domain.enums.ParticipationStatus;
import cat.udl.eps.softarch.demo.domain.enums.ReportStatus;
import cat.udl.eps.softarch.demo.repository.CategoryRepository;
import cat.udl.eps.softarch.demo.repository.InvitationStatusRepository;
import cat.udl.eps.softarch.demo.repository.MeetupStatusRepository;
import cat.udl.eps.softarch.demo.repository.MembershipStatusRepository;
import cat.udl.eps.softarch.demo.repository.NotificationTypeRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationStatusRepository;
import cat.udl.eps.softarch.demo.repository.ReportStatusRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

import java.util.Arrays;

@Configuration
public class DBInitialization {
    @Value("${default-password}")
    String defaultPassword;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationTypeRepository notificationTypeRepository;
    private final ReportStatusRepository reportStatusRepository;
    private final MeetupStatusRepository meetupStatusRepository;
    private final InvitationStatusRepository invitationStatusRepository;
    private final MembershipStatusRepository membershipStatusRepository;
    private final ParticipationStatusRepository participationStatusRepository;

    public DBInitialization(UserRepository userRepository, CategoryRepository categoryRepository,
                            NotificationTypeRepository notificationTypeRepository,
                            ReportStatusRepository reportStatusRepository,
                            MeetupStatusRepository meetupStatusRepository,
                            InvitationStatusRepository invitationStatusRepository,
                            MembershipStatusRepository membershipStatusRepository,
                            ParticipationStatusRepository participationStatusRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.notificationTypeRepository = notificationTypeRepository;
        this.reportStatusRepository = reportStatusRepository;
        this.meetupStatusRepository = meetupStatusRepository;
        this.invitationStatusRepository = invitationStatusRepository;
        this.membershipStatusRepository = membershipStatusRepository;
        this.participationStatusRepository = participationStatusRepository;
    }

    @PostConstruct
    public void initializeDatabase() {
        initializeCatalogs();
        // Default user
        if (!userRepository.existsById("demo")) {
            User user = new User();
            user.setEmail("demo@sample.app");
            user.setId("demo");
            user.setPassword("Azqw4321");
            user.encodePassword();
            userRepository.save(user);
        }
        if (Arrays.asList(activeProfiles.split(",")).contains("test")) {
            // Testing instances
            if (!userRepository.existsById("test")) {
                User user = new User();
                user.setEmail("test@sample.app");
                user.setId("test");
                user.setPassword(defaultPassword);
                user.encodePassword();
                user = userRepository.save(user);
            }
        }
    }

    /**
     * Default values of the catalog tables. They are the former enum constants, so existing data and clients
     * keep working while new values can be added at runtime through the Spring Data REST endpoints.
     */
    private void initializeCatalogs() {
        seedCategory("TECHNOLOGY", "Technology and gadgets", "#1E88E5");
        seedCategory("SPORTS", "Sport and outdoor activities", "#43A047");
        seedCategory("ARTS", "Arts and crafts", "#8E24AA");
        seedCategory("CULTURE", "Culture and leisure", "#F4511E");
        seedCategory("EDUCATION", "Learning and education", "#3949AB");
        seedCategory("HEALTH", "Health and wellness", "#00897B");
        seedCategory("OTHER", "Other categories", "#757575");

        seedNotificationType("INVITATION", "You have been invited to a meetup", "mail");
        seedNotificationType("MEETUP_CANCELLED", "A meetup you joined was cancelled", "cancel");
        seedNotificationType("MEETUP_UPDATED", "One of your meetups changed", "edit");
        seedNotificationType("SPOT_FREED", "A spot is available for you", "event_available");
        seedNotificationType("GROUP_REQUEST", "Someone asked to join your group", "group_add");

        seedReportStatus("OPEN", "Pending moderation", "#FB8C00");
        seedReportStatus("RESOLVED", "Report resolved", "#43A047");
        seedReportStatus("DISMISSED", "Report dismissed", "#757575");

        seedMeetupStatus("DRAFT", "Meetup not published yet", "#9E9E9E", true);
        seedMeetupStatus("OPEN", "Meetup accepting participants", "#43A047", true);
        seedMeetupStatus("FULL", "Meetup without free spots", "#FB8C00", true);
        seedMeetupStatus("CANCELLED", "Meetup cancelled", "#E53935", false);
        seedMeetupStatus("FINISHED", "Meetup already celebrated", "#1E88E5", false);

        seedInvitationStatus("PENDING", "Invitation awaiting an answer", 7);
        seedInvitationStatus("ACCEPTED", "Invitation accepted", null);
        seedInvitationStatus("DECLINED", "Invitation declined", null);

        seedMembershipStatus("PENDING", "Membership awaiting approval", 30);
        seedMembershipStatus("ACTIVE", "Active membership", null);

        seedParticipationStatus("CONFIRMED", "Confirmed participant", null);
        seedParticipationStatus("WAITING", "Waiting for a free spot", 30);
        seedParticipationStatus("CANCELLED", "Participation cancelled", null);
    }

    private void seedCategory(String id, String description, String color) {
        if (categoryRepository.existsById(id)) return;
        Category category = new Category(id);
        category.setDescription(description);
        category.setColor(color);
        categoryRepository.save(category);
    }

    private void seedNotificationType(String id, String description, String icon) {
        if (notificationTypeRepository.existsById(id)) return;
        NotificationType type = new NotificationType(id);
        type.setDescription(description);
        type.setIcon(icon);
        notificationTypeRepository.save(type);
    }

    private void seedReportStatus(String id, String description, String color) {
        if (reportStatusRepository.existsById(id)) return;
        ReportStatus status = new ReportStatus(id);
        status.setDescription(description);
        status.setColor(color);
        reportStatusRepository.save(status);
    }

    private void seedMeetupStatus(String id, String description, String color, boolean allowsComments) {
        if (meetupStatusRepository.existsById(id)) return;
        MeetupStatus status = new MeetupStatus(id);
        status.setDescription(description);
        status.setColor(color);
        status.setAllowsComments(allowsComments);
        meetupStatusRepository.save(status);
    }

    private void seedInvitationStatus(String id, String description, Integer expirationDays) {
        if (invitationStatusRepository.existsById(id)) return;
        InvitationStatus status = new InvitationStatus(id);
        status.setDescription(description);
        status.setExpirationDays(expirationDays);
        invitationStatusRepository.save(status);
    }

    private void seedMembershipStatus(String id, String description, Integer expirationDays) {
        if (membershipStatusRepository.existsById(id)) return;
        MembershipStatus status = new MembershipStatus(id);
        status.setDescription(description);
        status.setExpirationDays(expirationDays);
        membershipStatusRepository.save(status);
    }

    private void seedParticipationStatus(String id, String description, Integer expirationDays) {
        if (participationStatusRepository.existsById(id)) return;
        ParticipationStatus status = new ParticipationStatus(id);
        status.setDescription(description);
        status.setExpirationDays(expirationDays);
        participationStatusRepository.save(status);
    }
}
