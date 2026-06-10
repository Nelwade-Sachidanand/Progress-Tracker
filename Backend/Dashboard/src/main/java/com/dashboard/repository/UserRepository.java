package com.dashboard.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dashboard.entity.User;
import com.dashboard.model.UserModel;

public interface UserRepository extends MongoRepository<User, String> {

	Optional<User> findByUsername(String username);

	List<User> findByProjectNamesContaining(String projectName);
    List<User> findByRole(String role);

}