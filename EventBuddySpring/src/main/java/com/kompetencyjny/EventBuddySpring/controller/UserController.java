package com.kompetencyjny.EventBuddySpring.controller;

import com.kompetencyjny.EventBuddySpring.dto.UserDto;
import com.kompetencyjny.EventBuddySpring.model.User;
import com.kompetencyjny.EventBuddySpring.repo.UserRepository;
import com.kompetencyjny.EventBuddySpring.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        UserDto dto = new UserDto(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());
        return ResponseEntity.ok(dto);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String,String>> uploadAvatar(
            @RequestParam("avatar") MultipartFile avatar,
            @AuthenticationPrincipal UserDetails principal
    ) {
        String email = principal.getUsername();
        String url = userService.updateAvatar(email, avatar);
        return ResponseEntity.ok(Map.of("avatarUrl", url));
    }

    @PutMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String,String>> updateProfile(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @AuthenticationPrincipal UserDetails principal
    ) {
        var u = userService.updateProfile(principal.getUsername(), firstName, lastName);
        return ResponseEntity.ok(Map.of(
                "firstName", u.getFirstName(),
                "lastName",  u.getLastName(),
                "avatarUrl", u.getAvatarUrl() == null ? "" : u.getAvatarUrl()
        ));
    }

    @GetMapping
    public ResponseEntity<Map<String,String>> me(@AuthenticationPrincipal UserDetails principal) {
        var u = userService.findByEmail(principal.getUsername()).orElseThrow();
        return ResponseEntity.ok(Map.of(
                "email",     u.getEmail(),
                "firstName", u.getFirstName(),
                "lastName",  u.getLastName(),
                "avatarUrl", u.getAvatarUrl() == null ? "" : u.getAvatarUrl()
        ));
    }

}
