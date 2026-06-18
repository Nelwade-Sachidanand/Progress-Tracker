package com.novillex.progresstracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.novillex.progresstracker.entity.User;
import com.novillex.progresstracker.model.UserModel;

public interface UserRepository extends MongoRepository<User, String> {

	Optional<User> findByUsername(String username);

	List<User> findByProjectIdsContaining(String projectId);
    List<User> findByRole(String role);

}