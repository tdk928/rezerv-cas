package bg.rezerv.cas.web;

import bg.rezerv.cas.service.AuthService;
import bg.rezerv.cas.web.dto.AuthResponse;
import bg.rezerv.cas.web.dto.LoginRequest;
import bg.rezerv.cas.web.dto.RefreshRequest;
import bg.rezerv.cas.web.dto.RegisterRequest;
import bg.rezerv.cas.web.dto.UserResponse;
import bg.rezerv.cas.web.error.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gateway route: /api/auth/** → тук като /auth/** (StripPrefix маха /api). */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @GetMapping("/me")
    public UserResponse me(@RequestHeader(name = ContextHeaders.USER_ID, required = false) Long userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Липсва автентикация");
        }
        return authService.me(userId);
    }
}
