package ma.urbanops.service;

import ma.urbanops.dto.request.RegisterRequest;
import ma.urbanops.entity.User;
import ma.urbanops.enums.Role;
import ma.urbanops.exception.ResourceNotFoundException;
import ma.urbanops.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    private RegisterRequest validRequest;
    private User testUser;

    @BeforeAll
    static void initAll() {
        System.out.println("=== Starting UserService Tests ===");
    }

    @BeforeEach
    void setUp() {
        validRequest = RegisterRequest.builder()
                .firstName("Yassine")
                .lastName("Benali")
                .email("yassine@test.ma")
                .password("pass123456")
                .phone("+212612345678")
                .sector("Guéliz")
                .receiveAlerts(true)
                .build();

        testUser = User.builder()
                .id(1L)
                .firstName("Yassine")
                .lastName("Benali")
                .email("yassine@test.ma")
                .password("$2a$hashedpassword")
                .role(Role.CITIZEN)
                .sector("Guéliz")
                .build();
    }

    @AfterAll
    static void cleanAll() {
        System.out.println("=== UserService Tests Complete ===");
    }

    @Test
    void register_withValidData_shouldSaveUserWithHashedPassword() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.register(validRequest);

        assertNotNull(result);
        assertEquals("Yassine", result.getFirstName());
        assertEquals("$2a$hashed", result.getPassword());
        assertNotSame(validRequest.getPassword(), result.getPassword());
    }

    @Test
    void register_withExistingEmail_shouldThrowException() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);
        
        assertThrows(IllegalArgumentException.class, () -> {
            userService.register(validRequest);
        });
    }

    @Test
    void findByEmail_whenExists_shouldReturnUser() {
        when(userRepository.findByEmail("yassine@test.ma")).thenReturn(Optional.of(testUser));
        
        User result = userService.findByEmail("yassine@test.ma");
        
        assertNotNull(result);
        assertEquals("yassine@test.ma", result.getEmail());
    }

    @Test
    void findByEmail_whenNotFound_shouldThrowException() {
        when(userRepository.findByEmail("unknown@test.ma")).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.findByEmail("unknown@test.ma");
        });
    }

    @Test
    void findById_whenExists_shouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        User result = userService.findById(1L);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findById_whenNotFound_shouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.findById(999L);
        });
    }

    @Test
    void deactivateUser_shouldMakeUserInactive() {
        User user = new User();
        user.setId(1L);
        user.setIsActive(true);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        userService.deactivateUser(1L);
        
        assertFalse(user.getIsActive());
        verify(userRepository).save(user);
    }

    @Test
    void countCitizens_shouldReturnCorrectCount() {
        when(userRepository.countByRole(Role.CITIZEN)).thenReturn(150L);
        
        long result = userService.countCitizens();
        
        assertEquals(150L, result);
    }

    @Test
    void countAll_shouldReturnTotalUsers() {
        when(userRepository.count()).thenReturn(200L);
        
        long result = userService.countAll();
        
        assertEquals(200L, result);
    }

    @Test
    void findAllAndAdminHelpers_shouldDelegateToRepository() {
        Pageable pageable = Pageable.ofSize(5);
        Page<User> page = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(pageable)).thenReturn(page);
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(testUser));
        when(userRepository.existsByEmail("admin@test.ma")).thenReturn(true);
        when(userRepository.save(testUser)).thenReturn(testUser);

        assertSame(page, userService.findAll(pageable));
        assertEquals(List.of(testUser), userService.findAllUsers());
        assertEquals(List.of(testUser), userService.findByRole(Role.ADMIN));
        assertTrue(userService.existsByEmail("admin@test.ma"));
        assertSame(testUser, userService.save(testUser));
    }

    @Test
    void updateProfile_shouldUpdatePasswordOnlyWhenProvided() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("New")
                .lastName("Name")
                .phone("0611")
                .sector("Medina")
                .receiveAlerts(false)
                .password("new-pass")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new");
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userService.updateProfile(1L, request);

        assertEquals("New", result.getFirstName());
        assertEquals("encoded-new", result.getPassword());
        assertFalse(result.getReceiveAlerts());
    }

    @Test
    void countActiveThisWeek_whenRepositoryReturnsNull_shouldReturnZero() {
        when(userRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(null);

        assertEquals(0L, userService.countActiveThisWeek());
    }

    @Test
    void deleteById_shouldDelegateToRepository() {
        userService.deleteById(7L);

        verify(userRepository).deleteById(7L);
    }
}
