package com.ticketmanagement.dto;

import com.ticketmanagement.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

    @NotBlank(message = "must not be blank")
    @Size(max = 120, message = "size must be at most 120")
    private String title;

    @NotBlank(message = "must not be blank")
    @Size(max = 4000, message = "size must be at most 4000")
    private String description;

    @NotNull(message = "must not be null")
    private TicketPriority priority;

    @Size(max = 80, message = "size must be at most 80")
    private String assignee;
}
