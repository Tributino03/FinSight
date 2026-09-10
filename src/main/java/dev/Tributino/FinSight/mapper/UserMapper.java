package dev.Tributino.FinSight.mapper;

import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.user.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}
