package com.example.asset.repository;

import com.example.asset.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("SELECT t FROM Ticket t " +
            "LEFT JOIN FETCH t.asset " +
            "LEFT JOIN FETCH t.raisedBy " +
            "LEFT JOIN FETCH t.technician " +
            "ORDER BY t.createdAt DESC")
    List<Ticket> findAllWithDetailsOrderByCreatedAtDesc();

    @Query("SELECT t FROM Ticket t " +
            "LEFT JOIN FETCH t.asset " +
            "LEFT JOIN FETCH t.raisedBy " +
            "LEFT JOIN FETCH t.technician " +
            "WHERE t.technician.id = :techId ORDER BY t.createdAt DESC")
    List<Ticket> findByTechnicianIdWithDetails(@Param("techId") Long techId);

    @Query("SELECT t FROM Ticket t " +
            "LEFT JOIN FETCH t.asset " +
            "LEFT JOIN FETCH t.raisedBy " +
            "LEFT JOIN FETCH t.technician " +
            "WHERE t.raisedBy.id = :employeeId ORDER BY t.createdAt DESC")
    List<Ticket> findByRaisedByIdWithDetailsOrderByCreatedAtDesc(@Param("employeeId") Long employeeId);

    @Query("SELECT t FROM Ticket t " +
            "LEFT JOIN FETCH t.asset " +
            "LEFT JOIN FETCH t.raisedBy " +
            "LEFT JOIN FETCH t.technician " +
            "WHERE t.id = :id")
    Optional<Ticket> findByIdWithDetails(@Param("id") Long id);
}
