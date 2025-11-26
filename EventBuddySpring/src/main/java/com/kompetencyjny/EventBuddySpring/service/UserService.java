package com.kompetencyjny.EventBuddySpring.service;

import com.google.firebase.database.annotations.Nullable;
import com.kompetencyjny.EventBuddySpring.exception.NotFoundException;
import com.kompetencyjny.EventBuddySpring.model.User;
import com.kompetencyjny.EventBuddySpring.model.Role;
import com.kompetencyjny.EventBuddySpring.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public UserService(UserRepository userRepository, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public User registerUser(String email, String password, String firstName, String lastName) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Użytkownik już istnieje!");
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.USER);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        return userRepository.save(user);
    }
    public boolean authenticateUser(String username, String password) {
        Optional<User> userOptional = userRepository.findByEmail(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return passwordEncoder.matches(password, user.getPassword());
        }
        return false;
    }

    public Optional<User> findById(Long id){
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isEmpty()) return userOptional;
        if (userOptional.get().getActive().equals(false)) return Optional.empty();
        return userOptional;
    }
    public Optional<User> findByEmail(String username){
        Optional<User> userOptional = userRepository.findByEmail(username);
        if (userOptional.isEmpty()) return userOptional;
        if (userOptional.get().getActive().equals(false)) return Optional.empty();
        return userOptional;
    }

    public Optional<User> findDeletedByUserName(String username){
        Optional<User> userOptional = userRepository.findByEmail(username);
        if (userOptional.isEmpty()) return userOptional;
        if (userOptional.get().getActive().equals(true)) return Optional.empty();
        return userOptional;
    }

    @Transactional
    public User setUserRole(String username, Role role){
        Optional<User> userOpt = this.findByEmail(username);
        if (userOpt.isEmpty()) throw new NotFoundException("User of username: "+username+" does not exist!");
        User user = userOpt.get();
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public String updateAvatar(String email, MultipartFile avatar) {
        validateImage(avatar);
        var user = userRepository.findByEmail(email).orElseThrow();
        String url = storeUserAvatar(email, avatar);
        user.setAvatarUrl(url);
        userRepository.save(user);
        return url;
    }

    @Transactional
    public User updateProfile(String email, String firstName, String lastName) {
        var user = userRepository.findByEmail(email).orElseThrow();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return userRepository.save(user);
    }

    private String storeUserAvatar(String email, MultipartFile avatar) {
        try {
            return fileStorageService.storeAvatar(avatar, email);
        } catch (IOException e) {
            throw new RuntimeException("Nie udało się zapisać avatara", e);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Brak pliku");
        String ct = file.getContentType();
        if (ct == null || !(ct.equals("image/jpeg") || ct.equals("image/png") || ct.equals("image/webp")))
            throw new IllegalArgumentException("Nieobsługiwany typ pliku");
        if (file.getSize() > 5 * 1024 * 1024)
            throw new IllegalArgumentException("Plik za duży (max 5MB)");
    }

}
