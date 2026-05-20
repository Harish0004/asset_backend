package com.example.asset.repository;

import com.example.asset.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Fetches all corporate system tickets safely ordered by newest creation date
    List<Ticket> findAllByOrderByCreatedAtDesc();

    // Fetches tickets assigned directly to a targeted technician profile context
    @Query("SELECT t FROM Ticket t WHERE t.technician.id = :techId ORDER BY t.createdAt DESC")
    List<Ticket> findByTechnicianId(@Param("techId") Long techId);

    // Fetches personal tickets written by a specific employee
    List<Ticket> findByRaisedByIdOrderByCreatedAtDesc(Long employeeId);
}