package com.nart.aicv.controllers;

import com.nart.aicv.exception.UnexpectedException;
import com.nart.aicv.exception.ValidationException;
import com.nart.aicv.requests.AIRequest;
import com.nart.aicv.servers.AIService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api")
public class AICvController {

    private final AIService aiService;

    public AICvController(AIService aiService) {
        this.aiService = aiService;
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/queryAI")
    public ResponseEntity<Map<String, String>> queryAI(@RequestBody @Valid AIRequest request) {
        try {
            String response = aiService.callAI(request.getMessage());
            // Always return a JSON response
            Map<String, String> jsonResponse = Map.of("response", response);
            return ResponseEntity.ok(jsonResponse);
        } catch (Exception e) {
            // Log the error for debugging
            e.printStackTrace();

            // Handle the exception and return a JSON error response
            Map<String, String> errorResponse = Map.of("response", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}
