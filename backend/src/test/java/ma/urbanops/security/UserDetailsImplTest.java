package ma.urbanops.security;

import ma.urbanops.entity.User;
import ma.urbanops.enums.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDetailsImplTest {

    @Test
    void build_shouldExposeSpringSecurityUserDetailsContract() {
        UserDetailsImpl details = UserDetailsImpl.build(User.builder()
                .id(1L)
                .firstName("Yassine")
                .lastName("Benali")
                .email("yassine@test.ma")
                .password("secret")
                .role(Role.MANAGER)
                .build());

        assertEquals("yassine@test.ma", details.getUsername());
        assertEquals("secret", details.getPassword());
        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isCredentialsNonExpired());
        assertTrue(details.isEnabled());
        assertEquals("ROLE_MANAGER", details.getAuthorities().iterator().next().getAuthority());
    }
}
