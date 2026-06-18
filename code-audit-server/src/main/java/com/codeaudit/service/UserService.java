package com.codeaudit.service;

import com.codeaudit.dto.LoginReq;
import com.codeaudit.dto.RegisterReq;
import com.codeaudit.entity.User;
import com.codeaudit.vo.LoginResp;

public interface UserService {
    LoginResp register(RegisterReq req);
    LoginResp login(LoginReq req);
    LoginResp refreshToken(String refreshToken);
    void logout(String accessToken);
    User currentUser();
}
