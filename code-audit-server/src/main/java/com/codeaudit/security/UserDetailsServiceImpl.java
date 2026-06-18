package com.codeaudit.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeaudit.entity.User;
import com.codeaudit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (Objects.isNull(user)) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return new LoginUser(user.getId(), user.getUsername(), user.getPassword(), user.getRole(), 1);
    }
}
