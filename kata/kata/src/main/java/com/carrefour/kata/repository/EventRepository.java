package com.carrefour.kata.repository;

import com.carrefour.kata.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}

