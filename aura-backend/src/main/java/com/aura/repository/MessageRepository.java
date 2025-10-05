package com.aura.repository;

import com.aura.domain.MessageEntity;
import com.aura.domain.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findBySessionOrderByTimestampAsc(SessionEntity session);
}
