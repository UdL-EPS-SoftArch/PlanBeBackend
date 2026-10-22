package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.repository.BlockRepository;
import cat.udl.eps.softarch.demo.repository.InvitationRepository;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.NotificationRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.ReportRepository;
import io.cucumber.java.Before;

/**
 * The Spring context (and its H2 database) is shared by all scenarios. Without this, meetups with the
 * same title accumulate (findByTitle fails with "query did not return a unique result") and
 * notifications from previous scenarios break the counts. Blocks and reports are removed too: they point to
 * users, who are kept (the steps reuse them), so they would leak into the next scenarios.
 */
public class DatabaseCleanupHooks {

    private final NotificationRepository notificationRepository;
    private final InvitationRepository invitationRepository;
    private final ParticipationRepository participationRepository;
    private final MeetupRepository meetupRepository;
    private final BlockRepository blockRepository;
    private final ReportRepository reportRepository;

    public DatabaseCleanupHooks(NotificationRepository notificationRepository, InvitationRepository invitationRepository,
                                ParticipationRepository participationRepository, MeetupRepository meetupRepository,
                                BlockRepository blockRepository, ReportRepository reportRepository) {
        this.notificationRepository = notificationRepository;
        this.invitationRepository = invitationRepository;
        this.participationRepository = participationRepository;
        this.meetupRepository = meetupRepository;
        this.blockRepository = blockRepository;
        this.reportRepository = reportRepository;
    }

    @Before(order = 0)
    public void cleanMeetupData() {
        notificationRepository.deleteAll();
        invitationRepository.deleteAll();
        participationRepository.deleteAllInBatch();
        meetupRepository.deleteAllInBatch();
        blockRepository.deleteAll();
        reportRepository.deleteAll();
    }
}
