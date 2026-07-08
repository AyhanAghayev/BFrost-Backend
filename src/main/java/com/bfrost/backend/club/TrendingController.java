package com.bfrost.backend.club;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trending")
@RequiredArgsConstructor
public class TrendingController {

    private final ClubRepository clubRepository;

    public record TrendingTopicDto(String id, String tag, String category, String summary) {}

    @GetMapping
    public List<TrendingTopicDto> trending() {
        return clubRepository.findTrendingTags(10).stream()
                .map(t -> new TrendingTopicDto(
                        t.getTag(),
                        "#" + t.getTag(),
                        t.getCategory(),
                        t.getCnt() + (t.getCnt() == 1 ? " club" : " clubs")))
                .toList();
    }
}