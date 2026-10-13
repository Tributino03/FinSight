package dev.Tributino.FinSight.mapper;

import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.user.AdminUserResponse;
import org.springframework.stereotype.Component;

@Component
public class AdminMapper {

    public AdminUserResponse toResponse(User adminUser){
        return new AdminUserResponse(
                adminUser.getId(),
                adminUser.getName(),
                adminUser.getEmail(),
                adminUser.getRole(),
                adminUser.isEnabled(),
                adminUser.getCreatedAt()
        );
    }
}
