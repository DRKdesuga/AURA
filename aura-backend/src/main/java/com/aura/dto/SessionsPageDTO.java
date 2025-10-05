package com.aura.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionsPageDTO {
    private List<SessionSummaryDTO> items;
    private long total;
    private int page;
    private int size;
}
