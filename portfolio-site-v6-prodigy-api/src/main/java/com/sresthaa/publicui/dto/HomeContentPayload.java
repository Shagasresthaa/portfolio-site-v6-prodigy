package com.sresthaa.publicui.dto;

import java.util.List;

import com.sresthaa.publicui.model.TimelineEvent;

public record HomeContentPayload(String aboutHook, List<String> aboutStory, List<TimelineEvent> timeline) {
}
