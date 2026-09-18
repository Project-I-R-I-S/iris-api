package com.iris.weight;

import com.iris.common.security.AuthenticatedUser;
import com.iris.user.UserService;
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
    private final UserService userService;

    public WeightController(WeightService weightService, UserService userService) {
        this.weightService = weightService;
        this.userService = userService;
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
            @RequestParam(required = false) String timezone
    ) {
        UUID userId = AuthenticatedUser.currentUserId();
        ZoneId zone = timezone != null ? ZoneId.of(timezone)
                : ZoneId.of(userService.getById(userId).timezone());
        return weightService.listInRange(userId, from, to, zone);
    }

    @GetMapping("/latest")
    public ResponseEntity<WeightEntryResponse> latest() {
        WeightEntryResponse result = weightService.latest(AuthenticatedUser.currentUserId());
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.noContent().build();
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
