package com.campus.agent.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@ApiModel(value = "登录结果", description = "登录成功后的返回信息")
public class LoginVo {

    @ApiModelProperty(value = "JWT Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @ApiModelProperty(value = "用户信息")
    private UserVo user;
}
