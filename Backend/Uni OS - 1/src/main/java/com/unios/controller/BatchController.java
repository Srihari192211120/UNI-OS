package com.unios.controller;

import com.unios.model.Batch;
import com.unios.repository.BatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/batches")
public class BatchController {

    @Autowired
    private BatchRepository batchRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Just in case, though SecurityConfig handles it too
    public ResponseEntity<Batch> createBatch(@RequestBody Batch batch) {
        // Status should be CREATED by default if not provided, or validated?
        // Basic implementation
        if (batch.getStatus() == null || batch.getStatus().isEmpty()) {
            batch.setStatus("CREATED");
        }
        return ResponseEntity.ok(batchRepository.save(batch));
    }

    @GetMapping("/active")
    public ResponseEntity<List<Batch>> getActiveBatches() {
        return ResponseEntity.ok(batchRepository.findByStatus("ACTIVE"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Batch> updateBatchStatus(@PathVariable Long id, @RequestBody Map<String, String> updates) {
        return batchRepository.findById(id)
                .map(batch -> {
                    if (updates.containsKey("status")) {
                        batch.setStatus(updates.get("status"));
                    }
                    return ResponseEntity.ok(batchRepository.save(batch));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
