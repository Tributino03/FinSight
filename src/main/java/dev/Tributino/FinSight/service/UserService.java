package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    public User create(User user) {
        validateEmailAvailability(user.getEmail());

        return this.userRepository.save(user);
    }

    public User update (Long id, User user) {

        User updatedUser = this.findById(id);

        updatedUser.setName(user.getName());
        updatedUser.setPassword(user.getPassword());

        return this.userRepository.save(updatedUser);
    }

    public void delete (Long id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    private void validateEmailAvailability(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("This email is already registered in the system.");
        }
    }
}
