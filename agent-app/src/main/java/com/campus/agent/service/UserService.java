package com.campus.agent.service;

import com.campus.agent.req.LoginReq;
import com.campus.agent.req.RegisterReq;
import com.campus.agent.vo.LoginVo;
import com.campus.agent.vo.UserVo;

public interface UserService {
    LoginVo login(LoginReq req);
    UserVo register(RegisterReq req);
    UserVo getCurrentUser(Long userId);
}
