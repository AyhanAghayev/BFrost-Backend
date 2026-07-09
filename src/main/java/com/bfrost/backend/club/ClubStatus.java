package com.bfrost.backend.club;

// Public clubs start PENDING and go live once an admin approves them.
// Private clubs are created APPROVED. (Rejected clubs are deleted, not stored.)
public enum ClubStatus { PENDING, APPROVED }
