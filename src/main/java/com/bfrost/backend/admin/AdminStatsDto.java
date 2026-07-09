package com.bfrost.backend.admin;

public record AdminStatsDto(
        long users,
        long clubsTotal,
        long clubsApproved,
        long clubsPending,
        long posts,
        long events,
        long memberships
) {}
