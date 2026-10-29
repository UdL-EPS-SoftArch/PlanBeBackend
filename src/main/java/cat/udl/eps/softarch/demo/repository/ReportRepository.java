package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.Report;
import cat.udl.eps.softarch.demo.domain.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource
public interface ReportRepository extends CrudRepository<Report, Long>, PagingAndSortingRepository<Report, Long> {

    /** A user sees only their own reports (#49), an ADMIN all of them (#50). */
    String OWNERSHIP_CLAUSE = "r.reporter.id = ?#{authentication.name} OR ?#{hasRole('ADMIN')} = true";

    @Override
    @Query("SELECT r FROM Report r WHERE " + OWNERSHIP_CLAUSE)
    Page<Report> findAll(Pageable pageable);

    @Override
    @Query("SELECT r FROM Report r WHERE r.id = :id AND (" + OWNERSHIP_CLAUSE + ")")
    Optional<Report> findById(@Param("id") Long id);

    /** GET /reports/search/findByStatus?status=OPEN : the ADMIN moderation queue (a user gets only their own). */
    @Query("SELECT r FROM Report r WHERE r.status = :status AND (" + OWNERSHIP_CLAUSE + ")")
    Page<Report> findByStatus(@Param("status") ReportStatus status, Pageable pageable);
}
