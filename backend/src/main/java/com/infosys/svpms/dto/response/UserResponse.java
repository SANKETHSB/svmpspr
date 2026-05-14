package com.infosys.svpms.dto.response;
import com.infosys.svpms.entity.User;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private User.Role role;
    /** AC #4: Custom role name if assigned, overrides enum role */
    private String customRoleName;
    /** Effective role — customRoleName if set, otherwise role.name() */
    private String effectiveRole;
    private boolean active;
    private boolean locked;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
