package com.backendev.userservice.integration.repository;

import com.backendev.userservice.entity.Roles;
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
class RolesRepositoryIntegrationTest {

    @Autowired
    private RolesRepository rolesRepository;

    @Autowired
    private UsersRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // Delete users first (foreign key constraint)
        userRepository.deleteAll();
        rolesRepository.deleteAll();
        entityManager.flush();
    }

    @Test
    void testFindByName_Success() {
        Roles role = new Roles();
        role.setName("ROLE_USER");
        rolesRepository.save(role);

        Optional<Roles> foundRole = rolesRepository.findByName("ROLE_USER");

        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getName()).isEqualTo("ROLE_USER");
    }

    @Test
    void testFindByName_NotFound() {
        Optional<Roles> foundRole = rolesRepository.findByName("ROLE_NONEXISTENT");
        assertThat(foundRole).isEmpty();
    }
}
