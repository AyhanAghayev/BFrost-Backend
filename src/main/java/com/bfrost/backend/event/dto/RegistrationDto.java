package com.bfrost.backend.event.dto;

import com.bfrost.backend.event.EventQuestion;
import com.bfrost.backend.event.QuestionType;
import com.bfrost.backend.event.RsvpStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

// Event registration-form shapes: questions, RSVP with answers, and organizer responses.
public final class RegistrationDto {
    private RegistrationDto() {}

    public record QuestionDto(
            UUID id, String label, String type, boolean required, int position, List<String> options
    ) {
        public static QuestionDto from(EventQuestion q) {
            return new QuestionDto(q.getId(), q.getLabel(), q.getType().name(),
                    q.isRequired(), q.getPosition(), q.getOptions());
        }
    }

    public record QuestionInput(
            @NotBlank @Size(max = 255) String label,
            @NotNull QuestionType type,
            boolean required,
            List<@Size(max = 255) String> options
    ) {}

    public record AnswerInput(@NotNull UUID questionId, @Size(max = 4000) String value) {}

    public record RsvpRequest(@NotNull RsvpStatus status, List<AnswerInput> answers) {}

    public record RsvpResultDto(String status) {}

    public record AnswerDto(UUID questionId, String label, String value) {}

    public record ResponseDto(
            UUID    userId,
            String  username,
            String  displayName,
            String  profilePictureUrl,
            String  status,
            boolean attended,
            List<AnswerDto> answers
    ) {}
}
