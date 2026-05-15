package com.infosys.svpms.service;

import com.infosys.svpms.dto.request.RoleRequest;
import com.infosys.svpms.dto.response.RoleResponse;
import com.infosys.svpms.entity.*;
import com.infosys.svpms.exception.BusinessException;
import com.infosys.svpms.exception.ResourceNotFoundException;
import com.infosys.svpms.repository.*;
import com.infosys.svpms.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private CustomRoleRepository roleRepo;

    @Mock
    private RolePermissionRepository permRepo;

    @Mock
    private RoleAssignmentHistoryRepository historyRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private RoleServiceImpl roleService;

    private User testUser;
    private CustomRole testRole;
    private RolePermission testPermission;
    private RoleRequest testRoleRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("admin@svpms.com")
                .name("Test Admin")
                .role(User.Role.ADMIN)
                .build();

        testRole = CustomRole.builder()
                .id(1L)
                .name("CUSTOM_ROLE")
                .description("Test Custom Role")
                .system(false)
                .active(true)
                .build();

        testPermission = RolePermission.builder()
                .id(1L)
                .role(testRole)
                .module("VENDORS")
                .canCreate(true)
                .canRead(true)
                .canUpdate(true)
                .canDelete(false)
                .build();

        RoleRequest.ModulePermissionRequest modulePermRequest = new RoleRequest.ModulePermissionRequest();
        modulePermRequest.setModule("VENDORS");
        modulePermRequest.setCanCreate(true);
        modulePermRequest.setCanRead(true);
        modulePermRequest.setCanUpdate(true);
        modulePermRequest.setCanDelete(false);

        testRoleRequest = new RoleRequest();
        testRoleRequest.setName("Custom Role");
        testRoleRequest.setDescription("Test Custom Role");
        testRoleRequest.setPermissions(Arrays.asList(modulePermRequest));
    }

    @Test
    void testCreateRole_Success() {
        // Arrange
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(roleRepo.existsByName(anyString())).thenReturn(false);
        when(roleRepo.save(any(CustomRole.class))).thenReturn(testRole);
        when(permRepo.save(any(RolePermission.class))).thenReturn(testPermission);
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), any(), anyString(), anyString());

        // Act
        RoleResponse response = roleService.createRole(testRoleRequest, "admin@svpms.com");

        // Assert
        assertNotNull(response);
        verify(roleRepo, times(1)).save(any(CustomRole.class));
        verify(permRepo, times(1)).save(any(RolePermission.class));
    }

    @Test
    void testCreateRole_DuplicateName_ThrowsException() {
        // Arrange
        when(roleRepo.existsByName(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            roleService.createRole(testRoleRequest, "admin@svpms.com");
        });
    }

    @Test
    void testUpdateRole_Success() {
        // Arrange
        when(roleRepo.findById(anyLong())).thenReturn(Optional.of(testRole));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(roleRepo.save(any(CustomRole.class))).thenReturn(testRole);
        doNothing().when(permRepo).deleteByRoleId(anyLong());
        when(permRepo.save(any(RolePermission.class))).thenReturn(testPermission);
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString(), anyString(), anyString());

        // Act
        RoleResponse response = roleService.updateRole(1L, testRoleRequest, "admin@svpms.com");

        // Assert
        assertNotNull(response);
        verify(roleRepo, times(1)).save(any(CustomRole.class));
        verify(permRepo, times(1)).deleteByRoleId(1L);
    }

    @Test
    void testUpdateRole_SystemRole_CannotRename() {
        // Arrange
        testRole.setSystem(true);
        testRole.setName("ADMIN");
        testRoleRequest.setName("NEW_NAME");
        when(roleRepo.findById(anyLong())).thenReturn(Optional.of(testRole));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            roleService.updateRole(1L, testRoleRequest, "admin@svpms.com");
        });
    }

    @Test
    void testDeleteRole_Success() {
        // Arrange
        when(roleRepo.findById(anyLong())).thenReturn(Optional.of(testRole));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(userRepo.countByRoleName(anyString())).thenReturn(0L);
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString(), any(), anyString());
        doNothing().when(permRepo).deleteByRoleId(anyLong());
        doNothing().when(roleRepo).delete(any(CustomRole.class));

        // Act
        roleService.deleteRole(1L, "admin@svpms.com");

        // Assert
        verify(roleRepo, times(1)).delete(any(CustomRole.class));
        verify(permRepo, times(1)).deleteByRoleId(1L);
    }

    @Test
    void testDeleteRole_SystemRole_ThrowsException() {
        // Arrange
        testRole.setSystem(true);
        when(roleRepo.findById(anyLong())).thenReturn(Optional.of(testRole));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            roleService.deleteRole(1L, "admin@svpms.com");
        });
    }

    @Test
    void testDeleteRole_UsersAssigned_ThrowsException() {
        // Arrange
        when(roleRepo.findById(anyLong())).thenReturn(Optional.of(testRole));
        when(userRepo.countByRoleName(anyString())).thenReturn(5L);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            roleService.deleteRole(1L, "admin@svpms.com");
        });
    }

    @Test
    void testGetAllRoles_Success() {
        // Arrange
        when(roleRepo.findAll()).thenReturn(Arrays.asList(testRole));
        when(permRepo.findByRoleId(anyLong())).thenReturn(Arrays.asList(testPermission));

        // Act
        List<RoleResponse> responses = roleService.getAllRoles();

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void testGetRoleById_Success() {
        // Arrange
        when(roleRepo.findById(anyLong())).thenReturn(Optional.of(testRole));
        when(permRepo.findByRoleId(anyLong())).thenReturn(Arrays.asList(testPermission));

        // Act
        RoleResponse response = roleService.getRoleById(1L);

        // Assert
        assertNotNull(response);
        assertEquals("CUSTOM_ROLE", response.getName());
    }

    @Test
    void testGetRoleById_NotFound_ThrowsException() {
        // Arrange
        when(roleRepo.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            roleService.getRoleById(1L);
        });
    }

    @Test
    void testAssignRoleToUser_Success() {
        // Arrange
        when(userRepo.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(historyRepo.save(any(RoleAssignmentHistory.class))).thenReturn(new RoleAssignmentHistory());
        when(userRepo.save(any(User.class))).thenReturn(testUser);
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString(), anyString(), anyString());

        // Act
        roleService.assignRoleToUser(1L, "PROCUREMENT_MANAGER", "Test reason", "admin@svpms.com");

        // Assert
        verify(userRepo, times(1)).save(any(User.class));
        verify(historyRepo, times(1)).save(any(RoleAssignmentHistory.class));
    }

    @Test
    void testAssignRoleToUser_InvalidRole_ThrowsException() {
        // Arrange
        when(userRepo.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            roleService.assignRoleToUser(1L, "INVALID_ROLE", "Test reason", "admin@svpms.com");
        });
    }

    @Test
    void testGetPermissionsForRole_Success() {
        // Arrange
        when(permRepo.findByRoleName(anyString())).thenReturn(Arrays.asList(testPermission));

        // Act
        List<RoleResponse.PermissionResponse> permissions = roleService.getPermissionsForRole("CUSTOM_ROLE");

        // Assert
        assertNotNull(permissions);
        assertEquals(1, permissions.size());
    }
}
