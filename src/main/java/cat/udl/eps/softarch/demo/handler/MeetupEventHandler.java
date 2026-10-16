package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.ParticipationStatus;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
public class MeetupEventHandler {

    @Autowired
    private ParticipationRepository participationRepository;

    @HandleBeforeSave
    public void handleBeforeSave(Meetup meetup) {
        if (meetup.getId() == null || meetup.getStatus() != Meetup.MeetupStatus.CANCELLED) {
            return;
        }

        participationRepository.findAll().forEach(participation -> {
            if (participation.getMeetup() != null
                    && participation.getMeetup().getId().equals(meetup.getId())
                    && participation.getStatus() != ParticipationStatus.CANCELLED) {
                participation.setStatus(ParticipationStatus.CANCELLED);
                participationRepository.save(participation);
            }
        });
    }
}
