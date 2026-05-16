package com.campus.agent.controller;

import com.campus.agent.req.LoginReq;
import com.campus.agent.req.RegisterReq;
import com.campus.agent.service.UserService;
import com.campus.agent.util.JwtUtil;
import com.campus.agent.vo.LoginVo;
import com.campus.agent.vo.UserVo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /** POST /auth/login — 登录 */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReq req) {
        try {
            LoginVo loginVo = userService.login(req);
            return ResponseEntity.ok(loginVo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /auth/register — 注册 */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterReq req) {
        try {
            UserVo userVo = userService.register(req);
            return ResponseEntity.ok(userVo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /auth/me — 获取当前用户信息（需 Token） */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "未登录"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Token 已过期"));
            }
            Long userId = jwtUtil.getUserId(token);
            UserVo userVo = userService.getCurrentUser(userId);
            return ResponseEntity.ok(userVo);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "认证失败：" + e.getMessage()));
        }
    }
}
