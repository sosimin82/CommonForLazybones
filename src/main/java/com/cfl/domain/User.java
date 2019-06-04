package com.cfl.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class User {
    private String userId;
    private String userType;
    private String tenantId;
    private String serviceName;
    private String userSequence;
    private Map<String, Authority> userToAuthorities;
}
