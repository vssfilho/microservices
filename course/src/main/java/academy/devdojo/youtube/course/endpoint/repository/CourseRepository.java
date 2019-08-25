package academy.devdojo.youtube.course.endpoint.repository;

import org.springframework.data.repository.PagingAndSortingRepository;

import academy.devdojo.youtube.course.endpoint.model.Course;

public interface CourseRepository extends PagingAndSortingRepository<Course, Long> {

}
