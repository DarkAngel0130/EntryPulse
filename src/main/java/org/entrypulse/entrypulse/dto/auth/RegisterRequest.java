package org.entrypulse.entrypulse.dto.auth;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.entrypulse.entrypulse.model.User;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @Email(message = "Email is not valid")
    @NotBlank(message = "Email cannot be blank")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    private String password;

    private String phoneNumber;

    private User.Role role;
}
