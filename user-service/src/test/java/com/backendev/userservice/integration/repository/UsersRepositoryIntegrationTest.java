package com.backendev.userservice.integration.repository;

import com.backendev.userservice.entity.Users;
import com.backendev.userservice.repository.RolesRepository;
import com.backendev.userservice.repository.UsersRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UsersRepositoryIntegrationTest {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RolesRepository rolesRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        usersRepository.deleteAll();
        entityManager.flush();
    }

    @Test
    void testFindByEmail_Success() {
        Users user = new Users();
        user.setEmail("john@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPassword("encoded_password");
        user.setPhone("1234567890");
        usersRepository.save(user);

        Optional<Users> foundUser = usersRepository.findByEmail("john@example.com");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("john@example.com");
        assertThat(foundUser.get().getFirstName()).isEqualTo("John");
    }

    @Test
    void testFindByEmail_NotFound() {
        Optional<Users> foundUser = usersRepository.findByEmail("nonexistent@example.com");

        assertThat(foundUser).isEmpty();
    }

    @Test
    void testExistsByEmail_False() {
        boolean exists = usersRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    void testExistsByEmail_AfterDelete() {
        Users user = new Users();
        user.setEmail("john@example.com");
        user.setFirstName("John");
        user.setPassword("password");
        Users savedUser = usersRepository.save(user);

        assertThat(usersRepository.existsByEmail("john@example.com")).isTrue();

        usersRepository.deleteById(savedUser.getId());

        assertThat(usersRepository.existsByEmail("john@example.com")).isFalse();
    }

}
