package com.infosys.svpms.service.impl;

import com.infosys.svpms.dto.request.UserRequest;
import com.infosys.svpms.dto.response.UserResponse;
import com.infosys.svpms.entity.User;
import com.infosys.svpms.exception.*;
import com.infosys.svpms.repository.UserRepository;
import com.infosys.svpms.service.AuditService;
import com.infosys.svpms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    @Override @Transactional
    public UserResponse create(UserRequest req, String actorEmail) {
        if (repo.existsByEmail(req.getEmail()))
            throw new DuplicateException("User already exists: " + req.getEmail());
        User actor = repo.findByEmail(actorEmail).orElseThrow();
        User u = User.builder()
            .name(req.getName()).email(req.getEmail())
            .password(encoder.encode(req.getPassword()))
            .role(req.getRole()).build();
        User saved = repo.save(u);
        audit.log(actor.getId(),"USER",actor.getName(),"USER_CREATED","User",saved.getId(),"Created user: "+saved.getEmail());
        return toResponse(saved);
    }

    @Override @Transactional
    public UserResponse update(Long id, UserRequest req, String actorEmail) {
        User u = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User","id",id));
        User actor = repo.findByEmail(actorEmail).orElseThrow();
        // AC #3: capture old values before mutation
        String oldValue = "name=" + u.getName() + ", role=" + u.getRole();
        if (req.getName() != null) u.setName(req.getName());
        if (req.getRole() != null) u.setRole(req.getRole());
        if (req.getPassword() != null && !req.getPassword().isBlank())
            u.setPassword(encoder.encode(req.getPassword()));
        User saved = repo.save(u);
        String newValue = "name=" + saved.getName() + ", role=" + saved.getRole();
        audit.log(actor.getId(),"USER",actor.getName(),"USER_UPDATED","User",id,
            oldValue, newValue, "Updated user: "+id);
        return toResponse(saved);
    }

    @Override
    public UserResponse getById(Long id) {
        return toResponse(repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User","id",id)));
    }

    @Override
    public Page<UserResponse> getAll(String search, User.Role role, Pageable pageable) {
        return repo.searchUsers(search, role, pageable).map(this::toResponse);
    }

    @Override @Transactional
    public void deactivate(Long id, String actorEmail) {
        User u = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User","id",id));
        User actor = repo.findByEmail(actorEmail).orElseThrow();
        u.setActive(false);
        repo.save(u);
        audit.log(actor.getId(),"USER",actor.getName(),"USER_DEACTIVATED","User",id,"Deactivated user: "+id);
    }

    @Override @Transactional
    public void unlock(Long id, String actorEmail) {
        User u = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User","id",id));
        User actor = repo.findByEmail(actorEmail).orElseThrow();
        u.setLocked(false);
        u.setFailedLoginCount(0);
        u.setLockedUntil(null);
        repo.save(u);
        audit.log(actor.getId(),"USER",actor.getName(),"USER_UNLOCKED","User",id,"Unlocked user: "+id);
    }

    @Override
    public User findByEmail(String email) {
        return repo.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User","email",email));
    }

    private UserResponse toResponse(User u) {
        String effectiveRole = (u.getCustomRoleName() != null && !u.getCustomRoleName().isBlank())
            ? u.getCustomRoleName()
            : (u.getRole() != null ? u.getRole().name() : "");
        return UserResponse.builder()
            .id(u.getId()).name(u.getName()).email(u.getEmail())
            .role(u.getRole()).customRoleName(u.getCustomRoleName())
            .effectiveRole(effectiveRole)
            .active(u.isActive()).locked(u.isLocked())
            .lastLoginAt(u.getLastLoginAt()).createdAt(u.getCreatedAt())
            .build();
    }
}
