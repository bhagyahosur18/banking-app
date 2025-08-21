package com.backendev.userservice.service;

import com.backendev.userservice.dto.UserDTO;
import com.backendev.userservice.dto.UserProfileDTO;
import com.backendev.userservice.dto.UserRegistrationRequest;
import com.backendev.userservice.dto.UserRegistrationResponse;
import com.backendev.userservice.entity.Roles;
import com.backendev.userservice.entity.Users;
import com.backendev.userservice.exception.UserAlreadyExistsException;
import com.backendev.userservice.exception.UserNotFoundException;
import com.backendev.userservice.mapper.UserMapper;
import com.backendev.userservice.repository.RolesRepository;
import com.backendev.userservice.repository.UsersRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UsersService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolesRepository rolesRepository;
    private final UserMapper userMapper;

    private static final Logger log = LoggerFactory.getLogger(UsersService.class);

    public UserRegistrationResponse registerUser(UserRegistrationRequest userRegistrationRequest) {
        if(usersRepository.existsByEmail(userRegistrationRequest.getEmail())){
            log.error("User already exists for email {}", userRegistrationRequest.getEmail());
            throw new UserAlreadyExistsException("User already exists.");
        }

        Users users = userMapper.toEntity(userRegistrationRequest);
        users.setPassword(passwordEncoder.encode(userRegistrationRequest.getPassword()));
        Users savedUsers = usersRepository.save(users);

        Set<Roles> roles = userRegistrationRequest.getRoles().stream()
                .map(roleName -> rolesRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                .collect(Collectors.toSet());

        savedUsers.setRoles(roles);
        usersRepository.save(savedUsers);

        return userMapper.toResponse(savedUsers);
    }

    public UserDTO findUserByID(Long id) {
        Users users = usersRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found."));
        return userMapper.toDto(users);
    }

    public UserDTO updateUser(Long id, @Valid UserProfileDTO updatedUser) {
        Users users = usersRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found."));
        userMapper.updateUserFromDto(updatedUser, users);
        Users savedUser = usersRepository.save(users);
        return userMapper.toDto(savedUser);
    }

}
