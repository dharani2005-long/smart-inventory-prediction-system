package com.smartinventory.entity;

import com.smartinventory.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

/**
 * A grantable authority (ADMIN / MANAGER / STAFF).
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private RoleName name;
}
