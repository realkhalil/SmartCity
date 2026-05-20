package ma.urbanops.controller;

import ma.urbanops.dto.request.ChangePasswordRequest;
import ma.urbanops.dto.request.CreateUserRequest;
import ma.urbanops.dto.request.UpdateUserRequest;
import ma.urbanops.dto.response.UserResponse;
import ma.urbanops.entity.User;
import ma.urbanops.enums.Role;
import ma.urbanops.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AdminUserController adminUserController;

    private User user;
    private UserResponse response;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("User")
                .email("admin@test.ma")
                .role(Role.ADMIN)
                .phone("0600000000")
                .sector("Gueliz")
                .receiveAlerts(true)
                .isActive(true)
                .build();
        response = UserResponse.builder()
                .id(1L)
                .email("admin@test.ma")
                .role(Role.ADMIN)
                .isActive(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllUsers_shouldMapUsers() {
        when(userService.findAllUsers()).thenReturn(List.of(user));
        when(userService.toResponse(user)).thenReturn(response);

        ResponseEntity<List<UserResponse>> result = adminUserController.getAllUsers();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("admin@test.ma", result.getBody().get(0).getEmail());
    }

    @Test
    void getByRole_shouldConvertRoleAndMapUsers() {
        when(userService.findByRole(Role.ADMIN)).thenReturn(List.of(user));
        when(userService.toResponse(user)).thenReturn(response);

        ResponseEntity<List<UserResponse>> result = adminUserController.getByRole("admin");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(userService).findByRole(Role.ADMIN);
    }

    @Test
    void createUser_whenEmailExists_shouldReturnBadRequest() {
        CreateUserRequest request = new CreateUserRequest("Ali", "Test", "ali@test.ma", "secret", null, null, null, null);
        when(userService.existsByEmail("ali@test.ma")).thenReturn(true);

        ResponseEntity<UserResponse> result = adminUserController.createUser(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verify(userService, never()).save(any());
    }

    @Test
    void createUser_shouldEncodePasswordAndUseDefaults() {
        CreateUserRequest request = new CreateUserRequest("Ali", "Test", "ali@test.ma", "secret", null, "0600", "Medina", null);
        when(userService.existsByEmail("ali@test.ma")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(userService.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(9L);
            return saved;
        });
        when(userService.toResponse(any(User.class))).thenReturn(UserResponse.builder().id(9L).email("ali@test.ma").build());

        ResponseEntity<UserResponse> result = adminUserController.createUser(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userCaptor.capture());
        assertEquals(Role.CITIZEN, userCaptor.getValue().getRole());
        assertTrue(userCaptor.getValue().getReceiveAlerts());
        assertEquals("encoded", userCaptor.getValue().getPassword());
    }

    @Test
    void updateUser_shouldOnlyApplyProvidedFields() {
        UpdateUserRequest request = new UpdateUserRequest("New", null, Role.MANAGER, null, "Medina");
        when(userService.findById(1L)).thenReturn(user);
        when(userService.save(user)).thenReturn(user);
        when(userService.toResponse(user)).thenReturn(response);

        ResponseEntity<UserResponse> result = adminUserController.updateUser(1L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("New", user.getFirstName());
        assertEquals(Role.MANAGER, user.getRole());
        assertEquals("Medina", user.getSector());
    }

    @Test
    void changePassword_shouldEncodeAndSave() {
        when(userService.findById(1L)).thenReturn(user);
        when(passwordEncoder.encode("new-secret")).thenReturn("encoded-new");

        ResponseEntity<Void> result = adminUserController.changePassword(1L, new ChangePasswordRequest("new-secret"));

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        assertEquals("encoded-new", user.getPassword());
        verify(userService).save(user);
    }

    @Test
    void deleteUser_shouldRejectDeletingCurrentUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@test.ma", "n/a"));
        when(userService.findByEmail("admin@test.ma")).thenReturn(user);

        ResponseEntity<Void> result = adminUserController.deleteUser(1L);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verify(userService, never()).deleteById(anyLong());
    }

    @Test
    void deleteUser_shouldDeleteAnotherUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@test.ma", "n/a"));
        when(userService.findByEmail("admin@test.ma")).thenReturn(user);

        ResponseEntity<Void> result = adminUserController.deleteUser(2L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(userService).deleteById(2L);
    }

    @Test
    void toggleActive_shouldFlipActiveFlag() {
        when(userService.findById(1L)).thenReturn(user);
        when(userService.save(user)).thenReturn(user);
        when(userService.toResponse(user)).thenReturn(response);

        ResponseEntity<UserResponse> result = adminUserController.toggleActive(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertFalse(user.getIsActive());
    }
}
