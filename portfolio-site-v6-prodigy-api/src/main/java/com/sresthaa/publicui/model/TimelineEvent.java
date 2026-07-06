package com.sresthaa.publicui.model;

// Not a JPA entity - HomeContent stores a list of these as a single JSON string column
// (see HomeContentService), matching the project's existing convention of storing simple
// structured lists (e.g. comma-separated tags) as one plain column rather than a child table,
// since this is always read/written as one whole blob, never queried by its own fields.
public record TimelineEvent(String title, String position, String institution, String date, String duration) {
}
