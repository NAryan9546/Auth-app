package com.pro.auth.Auth_app_backend.repositories;

import com.pro.auth.Auth_app_backend.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

}