package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.Review;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface ReviewRepository extends CrudRepository<Review, Long>, PagingAndSortingRepository<Review, Long> {

    Optional<Review> findByReviewerIdAndMeetupId(String reviewerId, Long meetupId);

    List<Review> findByMeetupId(Long meetupId);
}
