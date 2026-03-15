package com.example.bithumb.dto;

import java.sql.Date;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class SignUpDto {
    private Long userId;
    private String name;
    private String email;
    private String password;
    private String role;
    private String createdAt;
    private Date updatedAt;
}

