package com.examly.springapp.repository;

import com.examly.springapp.model.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for TicketComment entity.
 */
@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    List<TicketComment> findByTicketIdAndIsInternalFalseOrderByCreatedAtAsc(Long ticketId);

    long countByTicketId(Long ticketId);
}

