package com.ticketmanagement.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CreateCommentRequest {

    @NotBlank(message = "must not be blank")
    @Size(max = 80, message = "size must be at most 80")
    private String authorName;

    @NotBlank(message = "must not be blank")
    @Size(max = 2000, message = "size must be at most 2000")
    private String text;
}
