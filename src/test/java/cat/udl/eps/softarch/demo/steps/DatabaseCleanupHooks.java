package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.repository.InvitationRepository;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.NotificationRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import io.cucumber.java.Before;

/**
 * The Spring context (and its H2 database) is shared by all scenarios. Without this, meetups with the
 * same title accumulate (findByTitle fails with "query did not return a unique result") and
 * notifications from previous scenarios break the counts. Users are kept: the steps reuse them.
 */
public class DatabaseCleanupHooks {

    private final NotificationRepository notificationRepository;
    private final InvitationRepository invitationRepository;
    private final ParticipationRepository participationRepository;
    private final MeetupRepository meetupRepository;

    public DatabaseCleanupHooks(NotificationRepository notificationRepository, InvitationRepository invitationRepository,
                                ParticipationRepository participationRepository, MeetupRepository meetupRepository) {
        this.notificationRepository = notificationRepository;
        this.invitationRepository = invitationRepository;
        this.participationRepository = participationRepository;
        this.meetupRepository = meetupRepository;
    }

    @Before(order = 0)
    public void cleanMeetupData() {
        notificationRepository.deleteAll();
        invitationRepository.deleteAll();
        participationRepository.deleteAllInBatch();
        meetupRepository.deleteAllInBatch();
    }
}
