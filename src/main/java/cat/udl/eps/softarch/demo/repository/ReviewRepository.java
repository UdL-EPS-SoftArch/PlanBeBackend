package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.Review;
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
public interface ReviewRepository extends CrudRepository<Review, Long>, PagingAndSortingRepository<Review, Long> {

    // A review is visible exactly when its meetup is visible (#90)
    String VISIBLE_REVIEWS = "select r from Review r join r.meetup m where (" + MeetupRepository.VISIBILITY_CLAUSE + ")";

    @Override
    @Query(VISIBLE_REVIEWS)
    Page<Review> findAll(Pageable pageable);

    @Override
    @Query(VISIBLE_REVIEWS + " and r.id = :id")
    Optional<Review> findById(@Param("id") Long id);

    @Query(VISIBLE_REVIEWS + " and r.reviewer.id = :reviewerId and m.id = :meetupId")
    Optional<Review> findByReviewerIdAndMeetupId(@Param("reviewerId") String reviewerId,
                                                 @Param("meetupId") Long meetupId);

    @Query(VISIBLE_REVIEWS + " and m.id = :meetupId")
    List<Review> findByMeetupId(@Param("meetupId") Long meetupId);

    // Internal use only: the review handler checks duplicates whoever may read the meetup
    @RestResource(exported = false)
    @Query("select r from Review r where r.reviewer.id = :reviewerId and r.meetup.id = :meetupId")
    Optional<Review> findAnyByReviewerIdAndMeetupId(@Param("reviewerId") String reviewerId,
                                                    @Param("meetupId") Long meetupId);

    @Query("select new cat.udl.eps.softarch.demo.repository.ReviewRepository$AverageRating(avg(r.rating)) "
        + "from Review r where r.meetup.organizer.id = :organizerId")
    AverageRating findAverageRatingByOrganizer(@Param("organizerId") String organizerId);

    record AverageRating(Double average) {}
}
