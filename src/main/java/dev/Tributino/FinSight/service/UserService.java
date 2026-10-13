package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.user.RegisterRequest;
import dev.Tributino.FinSight.dto.user.UpdateUserRequest;
import dev.Tributino.FinSight.dto.user.UserResponse;
import dev.Tributino.FinSight.mapper.UserMapper;
import dev.Tributino.FinSight.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userMapper.toResponse(
                this.findEntityById(id)
        );
    }

    @Transactional(readOnly = true)
    User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found")
                );
    }

    @Transactional
    public UserResponse create(RegisterRequest request) {

        validateEmailAvailability(request.email());

        User newUser = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password())
        );

        User savedUser = userRepository.save(newUser);

        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponse update(User loggedUser, UpdateUserRequest request) {

        if (request.name() != null && !request.name().isBlank()) {
            loggedUser.updateName(request.name());
        }

        if (request.password() != null && !request.password().isBlank()) {
            loggedUser.changePassword(
                    passwordEncoder.encode(request.password())
            );
        }

        User savedUser = userRepository.save(loggedUser);
        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public void delete(User loggedUser) {
        userRepository.delete(loggedUser);
    }

    private void validateEmailAvailability(String email) {

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "This email is already registered."
            );
        }
    }
}