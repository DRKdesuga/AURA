package com.aura.service;

import com.aura.domain.MessageAuthor;
import com.aura.domain.MessageEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContextComposerTest {

    @Test
    @DisplayName("compose: includes memory block and recent messages in order")
    void compose_includesMemoryAndRecentMessagesInOrder() {
        ContextComposer composer = new ContextComposer();
        MessageEntity user = MessageEntity.builder().author(MessageAuthor.USER).content("Hello").build();
        MessageEntity assistant = MessageEntity.builder().author(MessageAuthor.ASSISTANT).content("Hi there").build();

        String prompt = composer.compose(
                "SYS",
                "{\"facts\":[\"uses AURA\"]}",
                List.of(user, assistant),
                "Next question"
        );

        int memoryIndex = prompt.indexOf("[MEMORY_JSON]\n{\"facts\":[\"uses AURA\"]}");
        int userIndex = prompt.indexOf("USER: Hello");
        int assistantIndex = prompt.indexOf("ASSISTANT: Hi there");
        int currentUserIndex = prompt.indexOf("[USER_MESSAGE]\nNext question");

        assertThat(memoryIndex).isGreaterThan(-1);
        assertThat(userIndex).isGreaterThan(-1);
        assertThat(assistantIndex).isGreaterThan(-1);
        assertThat(currentUserIndex).isGreaterThan(-1);
        assertThat(userIndex).isLessThan(assistantIndex);
        assertThat(assistantIndex).isLessThan(currentUserIndex);
    }
}
