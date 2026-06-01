package com.cleardocs.model.jpa;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", length = 20, unique = true, nullable = false)
    private ERole name;

    public enum ERole {
        ROLE_USER,
        ROLE_REVIEWER,
        ROLE_APPROVER,
        ROLE_ADMIN,
        ROLE_AUDITOR
    }
}
