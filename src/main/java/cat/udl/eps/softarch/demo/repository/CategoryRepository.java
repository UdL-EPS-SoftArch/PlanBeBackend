package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.enums.Category;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** Catalog of interest and group categories, managed at runtime through /categories. */
@RepositoryRestResource
public interface CategoryRepository extends CrudRepository<Category, String>, PagingAndSortingRepository<Category, String> {
}
