package com.sresthaa.publicui.model;

// Not a JPA entity - HomeContent stores a list of these as one JSON string column, not a child table.
public record TimelineEvent(String title, String position, String institution, String date, String duration) {
}
