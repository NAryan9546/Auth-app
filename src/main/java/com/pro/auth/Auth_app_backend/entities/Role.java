package com.pro.auth.Auth_app_backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Entity
@Table(name = "use_role")
public class Role {
    @Id
    private UUID id = UUID.randomUUID();
    @Column( unique = true,nullable = false)
    private String name;

}
