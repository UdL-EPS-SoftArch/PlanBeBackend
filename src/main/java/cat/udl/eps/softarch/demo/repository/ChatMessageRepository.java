package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface ChatMessageRepository extends CrudRepository<ChatMessage, Long>, PagingAndSortingRepository<ChatMessage, Long> {

    /** Only confirmed participants of the meetup can read its chat. */
    String PARTICIPANT_CLAUSE =
        "exists (select p from Participation p where p.meetup = c.meetup " +
        "and p.participant.id = ?#{authentication.name} " +
        "and p.status = cat.udl.eps.softarch.demo.domain.enums.ParticipationStatus.CONFIRMED)";

    @Override
    @Query("SELECT c FROM ChatMessage c WHERE " + PARTICIPANT_CLAUSE + " ORDER BY c.sentAt ASC, c.id ASC")
    Page<ChatMessage> findAll(Pageable pageable);

    @Override
    @Query("SELECT c FROM ChatMessage c WHERE c.id = :id AND (" + PARTICIPANT_CLAUSE + ")")
    Optional<ChatMessage> findById(@Param("id") Long id);

    @Query("SELECT c FROM ChatMessage c WHERE c.meetup.id = :meetupId AND (" + PARTICIPANT_CLAUSE + ") "
        + "ORDER BY c.sentAt ASC, c.id ASC")
    Page<ChatMessage> findByMeetupId(@Param("meetupId") Long meetupId, Pageable pageable);

    @RestResource(exported = false)
    List<ChatMessage> findAllByMeetupId(Long meetupId);
}
