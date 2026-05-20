package ma.urbanops.controller;

import ma.urbanops.dto.response.UserResponse;
import ma.urbanops.entity.User;
import ma.urbanops.enums.Role;
import ma.urbanops.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserService userService;

    @InjectMocks private UserController userController;

    private User user;
    private UserResponse response;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("user@test.ma").role(Role.CITIZEN).build();
        response = UserResponse.builder().id(1L).email("user@test.ma").role(Role.CITIZEN).build();
    }

    @Test
    void getAllUsers_shouldReturnMappedPage() {
        Pageable pageable = Pageable.ofSize(5);
        when(userService.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user)));
        when(userService.toResponse(user)).thenReturn(response);

        assertEquals("user@test.ma", userController.getAllUsers(pageable).getBody().getContent().get(0).getEmail());
    }

    @Test
    void getUserById_shouldReturnMappedUser() {
        when(userService.findById(1L)).thenReturn(user);
        when(userService.toResponse(user)).thenReturn(response);

        assertEquals(Role.CITIZEN, userController.getUserById(1L).getBody().getRole());
    }

    @Test
    void deactivateUser_shouldReturnNoContent() {
        assertEquals(HttpStatus.NO_CONTENT, userController.deactivateUser(1L).getStatusCode());
        verify(userService).deactivateUser(1L);
    }

    @Test
    void getUserStats_shouldReturnCounts() {
        when(userService.countCitizens()).thenReturn(10L);
        when(userService.countActiveThisWeek()).thenReturn(4L);
        when(userService.countAll()).thenReturn(12L);

        Map<String, Long> body = userController.getUserStats().getBody();

        assertEquals(10L, body.get("totalCitizens"));
        assertEquals(4L, body.get("activeThisWeek"));
        assertEquals(12L, body.get("totalUsers"));
    }
}
