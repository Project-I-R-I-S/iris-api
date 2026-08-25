package com.iris.weight;

import com.iris.common.security.AuthenticatedUser;
import com.iris.weight.dto.WeightEntryRequest;
import com.iris.weight.dto.WeightEntryResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/weight/entries")
public class WeightController {

    private final WeightService weightService;

    public WeightController(WeightService weightService) {
        this.weightService = weightService;
    }

    @PostMapping
    public ResponseEntity<WeightEntryResponse> create(@RequestBody @Valid WeightEntryRequest req) {
        var created = weightService.create(AuthenticatedUser.currentUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<WeightEntryResponse> listInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "Asia/Kolkata") String timezone
    ) {
        return weightService.listInRange(
                AuthenticatedUser.currentUserId(), from, to, ZoneId.of(timezone));
    }

    @GetMapping("/latest")
    public WeightEntryResponse latest() {
        return weightService.latest(AuthenticatedUser.currentUserId());
    }

    @PutMapping("/{id}")
    public WeightEntryResponse update(@PathVariable UUID id, @RequestBody @Valid WeightEntryRequest req) {
        return weightService.update(AuthenticatedUser.currentUserId(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        weightService.delete(AuthenticatedUser.currentUserId(), id);
    }
}
