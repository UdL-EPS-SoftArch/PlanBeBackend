package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Participation;
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

        if (meetup.getStatus() == Meetup.MeetupStatus.FULL ||
            countConfirmed(meetup.getId()) >= meetup.getCapacity()) {
            throw new MeetupFullException("The meetup is full");
        }
    }

    @HandleAfterCreate
    @Transactional
    public void handleAfterCreate(Participation participation) {
        Meetup meetup = participation.getMeetup();
        if (meetup == null) return;

        long count = countConfirmed(meetup.getId());
        if (count >= meetup.getCapacity()) {
            meetup.setStatus(Meetup.MeetupStatus.FULL);
            meetupRepository.save(meetup);
        }
    }

    @HandleAfterDelete
    @Transactional
    public void handleAfterDelete(Participation participation) {
        Meetup meetup = participation.getMeetup();
        if (meetup == null) return;

        long count = countConfirmed(meetup.getId());
        if (count < meetup.getCapacity()) {
            meetup.setStatus(Meetup.MeetupStatus.OPEN);
            meetupRepository.save(meetup);
        }
    }

    private long countConfirmed(Long meetupId) {
        return StreamSupport.stream(participationRepository.findAll().spliterator(), false)
                .filter(p -> p.getMeetup() != null && p.getMeetup().getId().equals(meetupId))
                .count();
    }
}
