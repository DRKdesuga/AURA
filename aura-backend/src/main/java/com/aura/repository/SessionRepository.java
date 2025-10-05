package com.aura.repository;

import com.aura.domain.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<SessionEntity, Long> {
}
