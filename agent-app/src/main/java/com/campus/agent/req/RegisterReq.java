package com.campus.agent.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@ApiModel(value = "注册请求", description = "用户注册请求参数")
public class RegisterReq {

    @NotBlank(message = "用户名不能为空")
    @ApiModelProperty(value = "用户名", required = true, example = "zhangsan")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    @ApiModelProperty(value = "密码", required = true, example = "123456")
    private String password;

    @ApiModelProperty(value = "昵称", example = "张三")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @ApiModelProperty(value = "邮箱", example = "zhangsan@xxu.edu.cn")
    private String email;

    @ApiModelProperty(value = "手机号", example = "13800138000")
    private String phone;

    @ApiModelProperty(value = "班级", example = "计科2024-1班")
    private String studentClass;
}
