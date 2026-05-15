package com.infosys.svpms.service;

import com.infosys.svpms.dto.request.UserRequest;
import com.infosys.svpms.dto.response.UserResponse;
import com.infosys.svpms.entity.User;
import com.infosys.svpms.exception.DuplicateException;
import com.infosys.svpms.exception.ResourceNotFoundException;
import com.infosys.svpms.repository.UserRepository;
import com.infosys.svpms.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private User actorUser;
    private UserRequest userRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .role(User.Role.PROCUREMENT_MANAGER)
                .active(true)
                .locked(false)
                .failedLoginCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        actorUser = User.builder()
                .id(2L)
                .name("Admin User")
                .email("admin@example.com")
                .password("encodedPassword")
                .role(User.Role.ADMIN)
                .active(true)
                .build();

        userRequest = new UserRequest();
        userRequest.setName("New User");
        userRequest.setEmail("newuser@example.com");
        userRequest.setPassword("password123");
        userRequest.setRole(User.Role.PROCUREMENT_MANAGER);
    }

    @Test
    void testCreateUser_Success() {
        // Arrange
        when(userRepo.existsByEmail(anyString())).thenReturn(false);
        when(userRepo.findByEmail("admin@example.com")).thenReturn(Optional.of(actorUser));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepo.save(any(User.class))).thenReturn(testUser);

        // Act
        UserResponse result = userService.create(userRequest, "admin@example.com");

        // Assert
        assertNotNull(result);
        verify(userRepo).save(any(User.class));
        verify(auditService).log(anyLong(), eq("USER"), anyString(), eq("USER_CREATED"), 
                eq("User"), anyLong(), anyString());
    }

    @Test
    void testCreateUser_DuplicateEmail() {
        // Arrange
        when(userRepo.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateException.class, () -> {
            userService.create(userRequest, "admin@example.com");
        });
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void testUpdateUser_Success() {
        // Arrange
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepo.findByEmail("admin@example.com")).thenReturn(Optional.of(actorUser));
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
        when(userRepo.save(any(User.class))).thenReturn(testUser);

        // Act
        UserResponse result = userService.update(1L, userRequest, "admin@example.com");

        // Assert
        assertNotNull(result);
        verify(userRepo).save(any(User.class));
        verify(auditService).log(anyLong(), eq("USER"), anyString(), eq("USER_UPDATED"), 
                eq("User"), eq(1L), anyString(), anyString(), anyString());
    }

    @Test
    void testUpdateUser_NotFound() {
        // Arrange
        when(userRepo.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.update(999L, userRequest, "admin@example.com");
        });
    }

    @Test
    void testGetById_Success() {
        // Arrange
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        UserResponse result = userService.getById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Test User", result.getName());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testGetById_NotFound() {
        // Arrange
        when(userRepo.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getById(999L);
        });
    }

    @Test
    void testGetAll_Success() {
        // Arrange
        Page<User> userPage = new PageImpl<>(Arrays.asList(testUser));
        when(userRepo.searchUsers(anyString(), any(), any(Pageable.class))).thenReturn(userPage);

        // Act
        Page<UserResponse> result = userService.getAll("test", User.Role.PROCUREMENT_MANAGER, PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testDeactivateUser_Success() {
        // Arrange
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepo.findByEmail("admin@example.com")).thenReturn(Optional.of(actorUser));

        // Act
        userService.deactivate(1L, "admin@example.com");

        // Assert
        assertFalse(testUser.isActive());
        verify(userRepo).save(testUser);
        verify(auditService).log(anyLong(), eq("USER"), anyString(), eq("USER_DEACTIVATED"), 
                eq("User"), eq(1L), anyString());
    }

    @Test
    void testUnlockUser_Success() {
        // Arrange
        testUser.setLocked(true);
        testUser.setFailedLoginCount(5);
        testUser.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepo.findByEmail("admin@example.com")).thenReturn(Optional.of(actorUser));

        // Act
        userService.unlock(1L, "admin@example.com");

        // Assert
        assertFalse(testUser.isLocked());
        assertEquals(0, testUser.getFailedLoginCount());
        assertNull(testUser.getLockedUntil());
        verify(userRepo).save(testUser);
        verify(auditService).log(anyLong(), eq("USER"), anyString(), eq("USER_UNLOCKED"), 
                eq("User"), eq(1L), anyString());
    }

    @Test
    void testFindByEmail_Success() {
        // Arrange
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.findByEmail("test@example.com");

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testFindByEmail_NotFound() {
        // Arrange
        when(userRepo.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.findByEmail("notfound@example.com");
        });
    }
}
