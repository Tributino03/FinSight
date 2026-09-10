package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.user.RegisterRequest;
import dev.Tributino.FinSight.dto.user.UpdateUserRequest;
import dev.Tributino.FinSight.dto.user.UserResponse;
import dev.Tributino.FinSight.mapper.UserMapper;
import dev.Tributino.FinSight.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public UserResponse findById(Long id) {
        return userMapper.toResponse(
                this.findEntityById(id)
        );
    }

    User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found")
                );
    }

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

    public UserResponse update(
            Long id,
            UpdateUserRequest request
    ) {

        User updatedUser = this.findEntityById(id);

        if (request.name() != null && !request.name().isBlank()) {
            updatedUser.setName(request.name());
        }

        if (request.password() != null
                && !request.password().isBlank()) {

            updatedUser.setPassword(
                    passwordEncoder.encode(request.password())
            );
        }

        User savedUser = userRepository.save(updatedUser);

        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public void delete(Long id) {

        User user = this.findEntityById(id);

        userRepository.delete(user);
    }

    private void validateEmailAvailability(String email) {

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "This email is already registered in the system."
            );
        }
    }
}