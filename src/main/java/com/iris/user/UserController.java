package com.iris.user;

import com.iris.common.security.AuthenticatedUser;
import com.iris.user.dto.UpdateUserRequest;
import com.iris.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me() {
        return userService.getById(AuthenticatedUser.currentUserId());
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@RequestBody @Valid UpdateUserRequest req) {
        return userService.update(AuthenticatedUser.currentUserId(), req);
    }
}
