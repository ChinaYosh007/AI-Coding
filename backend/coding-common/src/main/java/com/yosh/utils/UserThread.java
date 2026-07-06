package com.yosh.utils;


import com.yosh.model.dto.user.UserDTO;

public class UserThread<T>{
    public final static ThreadLocal<UserDTO> user = new ThreadLocal<>();
    public static  void saveUser(UserDTO data){
        user.set(data);
    }
    public static UserDTO getUser(){
        return user.get();
    }
    public static void removeUser(){
        user.remove();
    }
}
