package com.iris.hydration;

import com.iris.common.security.AuthenticatedUser;
import com.iris.hydration.dto.DailyHydrationResponse;
import com.iris.hydration.dto.WaterEntryRequest;
import com.iris.hydration.dto.WaterEntryResponse;
import com.iris.user.UserService;
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
@RequestMapping("/api/v1/hydration")
public class HydrationController {

    private final HydrationService hydrationService;
    private final UserService userService;

    public HydrationController(HydrationService hydrationService, UserService userService) {
        this.hydrationService = hydrationService;
        this.userService = userService;
    }

    @PostMapping("/entries")
    public ResponseEntity<WaterEntryResponse> create(@RequestBody @Valid WaterEntryRequest req) {
        var created = hydrationService.create(AuthenticatedUser.currentUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/entries")
    public List<WaterEntryResponse> listForDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String timezone
    ) {
        UUID userId = AuthenticatedUser.currentUserId();
        ZoneId zone = timezone != null ? ZoneId.of(timezone)
                : ZoneId.of(userService.getById(userId).timezone());
        return hydrationService.listForDay(userId, date, zone);
    }

    @PutMapping("/entries/{id}")
    public WaterEntryResponse update(@PathVariable UUID id, @RequestBody @Valid WaterEntryRequest req) {
        return hydrationService.update(AuthenticatedUser.currentUserId(), id, req);
    }

    @DeleteMapping("/entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        hydrationService.delete(AuthenticatedUser.currentUserId(), id);
    }

    @GetMapping("/daily")
    public DailyHydrationResponse daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String timezone
    ) {
        UUID userId = AuthenticatedUser.currentUserId();
        ZoneId zone = timezone != null ? ZoneId.of(timezone)
                : ZoneId.of(userService.getById(userId).timezone());
        return hydrationService.dailyTotal(userId, date, zone);
    }
}
