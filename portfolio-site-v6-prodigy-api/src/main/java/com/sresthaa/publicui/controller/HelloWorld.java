package com.sresthaa.publicui.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorld {
	
	@GetMapping("/hello")
	public ResponseEntity<Map<String, String>> hello() {
	    return ResponseEntity.ok(Map.of("message", "hello"));
	}
	
}
