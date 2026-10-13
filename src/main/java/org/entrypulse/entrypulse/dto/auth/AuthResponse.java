package org.entrypulse.entrypulse.dto.auth;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String userId;
    private String name;
    private String role;
}
