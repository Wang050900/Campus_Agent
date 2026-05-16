package com.campus.agent.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "用户信息", description = "用户基本信息")
public class UserVo {

    @ApiModelProperty(value = "用户ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "用户名", example = "zhangsan")
    private String username;

    @ApiModelProperty(value = "昵称", example = "张三")
    private String nickname;

    @ApiModelProperty(value = "头像URL", example = "https://www.xxu.edu.cn/avatar/1.jpg")
    private String avatar;

    @ApiModelProperty(value = "邮箱", example = "zhangsan@xxu.edu.cn")
    private String email;

    @ApiModelProperty(value = "角色", example = "student")
    private String role;

    @ApiModelProperty(value = "班级", example = "计科2024-1班")
    private String studentClass;
}
