package com.dashboard.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dashboard.entity.User;

public interface UserRepository extends MongoRepository<User, String>{
	
	Optional<User> findByUsername(String username);

}
