package com.infosys.svpms.service;

import com.infosys.svpms.dto.request.UserRequest;
import com.infosys.svpms.dto.response.UserResponse;
import com.infosys.svpms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse create(UserRequest req, String actorEmail);
    UserResponse update(Long id, UserRequest req, String actorEmail);
    UserResponse getById(Long id);
    Page<UserResponse> getAll(String search, User.Role role, Pageable pageable);
    void deactivate(Long id, String actorEmail);
    void unlock(Long id, String actorEmail);
    User findByEmail(String email);
}
