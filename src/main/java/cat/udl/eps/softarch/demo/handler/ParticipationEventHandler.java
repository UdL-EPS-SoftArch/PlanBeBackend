package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.enums.MeetupStatus;
import cat.udl.eps.softarch.demo.exception.MeetupFullException;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.StreamSupport;

@Component
@RepositoryEventHandler
public class ParticipationEventHandler {

    @Autowired
    private MeetupRepository meetupRepository;

    @Autowired
    private ParticipationRepository participationRepository;

    @HandleBeforeCreate
    public void handleBeforeCreate(Participation participation) {
        Meetup meetup = participation.getMeetup();
        if (meetup == null) return;

        if (countConfirmed(meetup.getId()) >= meetup.getCapacity()) {
            participation.setStatus(Participation.ParticipationStatus.WAITING);
        }
    }

    @HandleAfterCreate
    @Transactional
    public void handleAfterCreate(Participation participation) {
        Meetup meetup = participation.getMeetup();
        if (meetup == null) return;

        long count = countConfirmed(meetup.getId());
        if (count >= meetup.getCapacity()) {
            meetupRepository.updateStatus(meetup.getId(), MeetupStatus.FULL);
        }
    }

    @HandleAfterDelete
    @Transactional
    public void handleAfterDelete(Participation participation) {
        Meetup meetup = participation.getMeetup();
        if (meetup == null) return;

        if (participation.getStatus() == Participation.ParticipationStatus.CONFIRMED) {
            Participation nextInLine = StreamSupport.stream(participationRepository.findAll().spliterator(), false)
                    .filter(p -> p.getMeetup() != null && p.getMeetup().getId().equals(meetup.getId()))
                    .filter(p -> p.getStatus() == Participation.ParticipationStatus.WAITING)
                    .sorted((p1, p2) -> p1.getId().compareTo(p2.getId()))
                    .findFirst()
                    .orElse(null);

            if (nextInLine != null) {
                nextInLine.setStatus(Participation.ParticipationStatus.CONFIRMED);
                participationRepository.save(nextInLine);

                // Ensure the meetup status is kept as FULL because the spot was immediately filled
                meetupRepository.updateStatus(meetup.getId(), MeetupStatus.FULL);
                return;
            }
        }

        long count = countConfirmed(meetup.getId());
        if (count < meetup.getCapacity()) {
            meetupRepository.updateStatus(meetup.getId(), MeetupStatus.OPEN);
        }
    }

    private long countConfirmed(Long meetupId) {
        return StreamSupport.stream(participationRepository.findAll().spliterator(), false)
                .filter(p -> p.getMeetup() != null && p.getMeetup().getId().equals(meetupId))
                .filter(p -> p.getStatus() == Participation.ParticipationStatus.CONFIRMED)
                .count();
    }
}
