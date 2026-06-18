package com.codeaudit.vo;

import com.codeaudit.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResp {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private UserInfo user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String role;
        private String email;
        private String realName;

        public static UserInfo from(User u) {
            return new UserInfo(u.getId(), u.getUsername(), u.getRole(), u.getEmail(), u.getRealName());
        }
    }
}
