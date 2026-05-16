package com.campus.agent.service.impl;

import com.campus.agent.entity.User;
import com.campus.agent.mapper.UserMapper;
import com.campus.agent.req.LoginReq;
import com.campus.agent.req.RegisterReq;
import com.campus.agent.service.UserService;
import com.campus.agent.util.JwtUtil;
import com.campus.agent.vo.LoginVo;
import com.campus.agent.vo.UserVo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public LoginVo login(LoginReq req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new RuntimeException("密码不能为空");
        }

        User user = userMapper.findByUsername(req.getUsername());
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVo(token, toUserVo(user));
    }

    @Override
    public UserVo register(RegisterReq req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            throw new RuntimeException("密码至少6位");
        }

        // 检查用户名是否已存在
        if (userMapper.findByUsername(req.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setStudentClass(req.getStudentClass());
        user.setRole("student");

        userMapper.insert(user);
        return toUserVo(user);
    }

    @Override
    public UserVo getCurrentUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return toUserVo(user);
    }

    private UserVo toUserVo(User user) {
        UserVo vo = new UserVo();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setStudentClass(user.getStudentClass());
        return vo;
    }
}
