package org.entrypulse.entrypulse.service;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.User;
import org.entrypulse.entrypulse.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;


    public User createUser(User user) {
        return userRepository.save(user);
    }


    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }
}
