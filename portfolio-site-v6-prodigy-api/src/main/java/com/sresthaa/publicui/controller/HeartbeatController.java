package com.sresthaa.publicui.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HeartbeatController {
	
	@GetMapping("/heartbeat")
	public ResponseEntity<Map<String, String>> heartbeat() {
	    return ResponseEntity.ok(Map.of("status", "OK"));
	}
	
}
