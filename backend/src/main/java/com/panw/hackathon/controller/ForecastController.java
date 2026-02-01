package com.panw.hackathon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panw.hackathon.model.ForecastResult;
import com.panw.hackathon.model.GoalRequest;
import com.panw.hackathon.model.Transaction;
import com.panw.hackathon.service.ForecastService;
import com.panw.hackathon.util.CsvParser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/forecast")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ForecastController {

    private final ForecastService forecastService = new ForecastService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ForecastResult analyze(
            @RequestPart("file") MultipartFile file,
            @RequestPart("goal") String goalJson
    ) throws IOException {
        List<Transaction> transactions = CsvParser.parseTransactions(file);
        GoalRequest goal = objectMapper.readValue(goalJson, GoalRequest.class);
        return forecastService.analyze(transactions, goal);
    }
}
