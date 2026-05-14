package com.infosys.svpms.dto.request;
import com.infosys.svpms.entity.User;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank @Size(max=100) public String name;
    @NotBlank @Email public String email;
    @Size(min=8, message="Password must be at least 8 characters") public String password;
    @NotNull public User.Role role;
}
