package com.pro.auth.Auth_app_backend.entities;

import org.hibernate.validator.constraints.UUID;

import java.lang.foreign.AddressLayout;
import java.time.Instant;
import java.util.HashSet;

public class user {
  private UUID id;
  private string email;
  private string name;
  private string password;
  private string image;
  private boolean enable = true;
  private Instant createAt=Instant.now();
  private Instant updatedAt=Instant.now();



  private Provider provider=Provider.LOCAL;
  enum Provider{ LOCAL,GOOGLE,GITHUB,FACEBOOK}

  private Set<Role> roles=new HashSet<>();

}
