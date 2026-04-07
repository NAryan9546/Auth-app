package com.pro.auth.Auth_app_backend.helpers;

import java.util.UUID;

public class UserHelper {
    public static UUID parseUUID(String uuid){
        return UUID.fromString(uuid);
    }
}
