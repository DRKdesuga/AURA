package com.aura.repository;

import com.aura.domain.MessageEntity;
import com.aura.domain.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findBySessionOrderByTimestampAsc(SessionEntity session);

    List<MessageEntity> findBySessionOrderByIdAsc(SessionEntity session);

    List<MessageEntity> findBySessionOrderByIdDesc(SessionEntity session, Pageable pageable);

    List<MessageEntity> findBySessionAndIdGreaterThanOrderByIdAsc(SessionEntity session, Long id);
}
