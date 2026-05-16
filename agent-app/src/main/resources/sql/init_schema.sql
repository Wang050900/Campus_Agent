-- campus_agent 数据库
CREATE DATABASE IF NOT EXISTS campus_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_agent;

-- 用户表
CREATE TABLE `user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password_hash` VARCHAR(255) NOT NULL,
  `nickname` VARCHAR(50) DEFAULT NULL,
  `avatar` VARCHAR(255) DEFAULT NULL,
  `email` VARCHAR(100) DEFAULT NULL,
  `phone` VARCHAR(20) DEFAULT NULL,
  `role` VARCHAR(20) NOT NULL DEFAULT 'student',
  `student_class` VARCHAR(50) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 校园文章表（RAG 知识库）
CREATE TABLE `campus_article` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(255) NOT NULL,
  `url` VARCHAR(500) DEFAULT NULL,
  `content` TEXT,
  `category` VARCHAR(50) DEFAULT NULL,
  `publish_date` DATE DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FULLTEXT INDEX ft_content (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 课程表
CREATE TABLE `course_schedule` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `class_name` VARCHAR(50) NOT NULL COMMENT '班级名称',
  `weekday` INT NOT NULL COMMENT '星期几 1-7',
  `course_name` VARCHAR(100) NOT NULL,
  `start_time` VARCHAR(20) NOT NULL COMMENT '如 08:00',
  `end_time` VARCHAR(20) NOT NULL COMMENT '如 09:40',
  `location` VARCHAR(100) DEFAULT NULL,
  `teacher` VARCHAR(50) DEFAULT NULL,
  `weeks` VARCHAR(50) DEFAULT NULL COMMENT '如 1-16 表示第1到16周',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 校园新闻表
CREATE TABLE `campus_news` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(255) NOT NULL,
  `content` TEXT,
  `category` VARCHAR(50) DEFAULT NULL COMMENT 'general/academic/activity/notice',
  `publish_date` DATE DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 空闲教室表
CREATE TABLE `classroom` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `building` VARCHAR(50) NOT NULL COMMENT '教学楼名称',
  `room_number` VARCHAR(20) NOT NULL COMMENT '房间号',
  `capacity` INT DEFAULT NULL,
  `has_projector` TINYINT(1) DEFAULT 0,
  `has_ac` TINYINT(1) DEFAULT 0,
  `is_free` TINYINT(1) DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== 种子数据 ======

-- 插入示例用户（密码均为 "123456" 的 BCrypt 哈希）
INSERT INTO `user` (username, password_hash, nickname, role, student_class) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员', 'admin', NULL),
('zhangsan', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '张三', 'student', '计科2024-1班'),
('lisi', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李四', 'student', '计科2024-1班');

-- 插入校园文章（RAG 知识库）
INSERT INTO campus_article (title, url, content, category, publish_date) VALUES
('2025-2026学年第二学期体育选修课选课通知', 'https://www.xxu.edu.cn/sports/2025/1210/c1234a56789/page.htm',
'各学院、各位同学：\n2025-2026学年第二学期体育选修课选课工作即将开始，现将有关事项通知如下：\n一、选课时间：2026年5月15日—6月1日\n二、选课方式：登录教务系统(http://jwxt.xxu.edu.cn)，进入"选课中心"\n三、开设课程：\n  1. 篮球——南操场（限80人）\n  2. 足球——北操场（限60人）\n  3. 羽毛球——体育馆（限40人）\n  4. 乒乓球——体育馆（限30人）\n  5. 游泳——体育馆游泳馆（限20人）\n  6. 瑜伽——体育馆舞蹈室（限30人）\n  7. 太极拳——南操场（限50人）\n四、每位同学限选1门\n五、选课结果将于6月5日公布\n六、如有疑问请联系体育部办公室（体育馆202室）',
 'notice', '2026-05-10');

INSERT INTO campus_article (title, url, content, category, publish_date) VALUES
('图书馆关于2026年端午节期间开放安排的通知', 'https://www.xxu.edu.cn/lib/2026/0510/c5678d12345/page.htm',
'各位读者：\n根据学校《关于2026年端午节放假安排的通知》，图书馆开放时间安排如下：\n一、6月8日（周六）至6月10日（周一）放假调休，共3天\n二、主馆1楼自习室正常开放（24小时）\n三、2-6楼阅览室开放时间调整为8:00-17:00\n四、6月11日（周二）起恢复正常开放\n五、放假期间，电子资源24小时正常访问',
 'notice', '2026-05-10');

INSERT INTO campus_article (title, url, content, category, publish_date) VALUES
('关于开放通宵自习室的通知', 'https://www.xxu.edu.cn/hqfw/2026/0301/c2345d67890/page.htm',
'各位同学：\n为满足广大同学的学习需求，学校决定开放通宵自习室。具体安排如下：\n一、开放地点：教学楼A栋1楼101-105教室\n二、开放时间：每天22:00-次日07:00\n三、注意事项：\n  1. 请保持安静，手机调至静音\n  2. 禁止占座，离开30分钟以上请带走个人物品\n  3. 保持教室卫生，离开时请带走垃圾\n  4. 通宵自习室只供自习使用，禁止进行与学习无关的活动',
 'notice', '2026-03-01');

INSERT INTO campus_article (title, url, content, category, publish_date) VALUES
('计算机学院2026年暑期实训项目报名通知', 'https://www.xxu.edu.cn/cs/2026/0420/c3456d78901/page.htm',
'各位计算机学院同学：\n为提升同学们的项目实践能力，学院将开展2026年暑期实训项目。具体如下：\n一、实训方向：\n  1. AI应用开发——基于大模型构建智能应用\n  2. 前后端全栈开发——Spring Boot + Vue.js\n  3. 数据分析与可视化——Python + ECharts\n二、时间：2026年7月10日—8月10日\n三、地点：计算机学院实验楼\n四、报名截止：2026年6月15日\n五、报名方式：发送"姓名+学号+所选方向"至实训中心邮箱 practice@xxu.edu.cn',
 'academic', '2026-04-20');

INSERT INTO campus_article (title, url, content, category, publish_date) VALUES
('校园超市2026年夏季营业时间调整通知', 'https://www.xxu.edu.cn/hqfw/2026/0512/c1357d45678/page.htm',
'各位师生：\n自2026年5月15日起，校园超市执行夏季营业时间，具体如下：\n南食堂负一楼店：7:00-23:00\n北食堂1楼店：7:00-22:30\n夏季优惠活动：每日20:00后水果8折优惠！\n欢迎大家选购。',
 'notice', '2026-05-12');

INSERT INTO campus_article (title, url, content, category, publish_date) VALUES
('校园科技文化节活动预告', 'https://www.xxu.edu.cn/activity/2026/0425/c4567d89012/page.htm',
'各位师生：\n"创新·融合·未来"2026年校园科技文化节即将开幕！\n时间：2026年5月20日—6月1日\n地点：各活动场地详见下方\n活动安排：\n一、开幕式暨科技成果展（5月20日 体育馆）\n二、AI创新大赛（5月22日 计算机学院）\n三、校园歌手大赛决赛（5月30日 体育馆）\n四、社团文化节（5月25日 南广场）\n五、闭幕式暨颁奖典礼（6月1日 大礼堂）\n欢迎广大师生积极参与！',
 'activity', '2026-04-25');

-- 插入课程数据（计科2024-1班）
INSERT INTO course_schedule (class_name, weekday, course_name, start_time, end_time, location, teacher, weeks) VALUES
('计科2024-1班', 1, '数据结构', '08:00', '09:40', '教学楼301', '张明', '1-16'),
('计科2024-1班', 1, '高等数学', '10:10', '12:00', '教学楼205', '李华', '1-16'),
('计科2024-1班', 1, '大学英语', '14:00', '15:40', '教学楼108', '王芳', '1-16'),
('计科2024-1班', 2, '计算机网络', '08:00', '09:40', '实验楼302', '赵强', '1-16'),
('计科2024-1班', 2, '操作系统', '10:10', '12:00', '教学楼406', '刘伟', '1-16'),
('计科2024-1班', 2, '体育', '16:00', '17:40', '南操场', '陈教练', '1-16'),
('计科2024-1班', 3, '数据结构（实验）', '08:00', '09:40', '实验楼505', '张明', '1-16'),
('计科2024-1班', 3, '高等数学', '10:10', '12:00', '教学楼205', '李华', '1-16'),
('计科2024-1班', 3, 'Python编程', '14:00', '15:40', '机房301', '周婷', '1-16'),
('计科2024-1班', 4, '软件工程', '08:00', '09:40', '教学楼302', '黄磊', '1-16'),
('计科2024-1班', 4, '数据库原理', '10:10', '12:00', '教学楼401', '孙梅', '1-16'),
('计科2024-1班', 5, '计算机网络（实验）', '08:00', '09:40', '实验楼303', '赵强', '1-16'),
('计科2024-1班', 5, '形势与政策', '14:00', '15:40', '教学楼101', '辅导员', '1-16');

-- 插入校园新闻
INSERT INTO campus_news (title, content, category, publish_date) VALUES
('我校荣获2026年全国创新创业大赛金奖', '由计算机学院和商学院联合团队研发的"智慧校园"项目在全国总决赛中脱颖而出，荣获金奖。', 'general', '2026-05-14'),
('2026年校园开放日定于6月15日举行', '欢迎广大考生和家长来校参观，届时各学院将设立咨询点。', 'general', '2026-05-12'),
('计算机学院举办人工智能学术讲座', '特邀清华大学李教授主讲"大模型时代的机遇与挑战"。', 'academic', '2026-05-16'),
('2026年校级科研项目申报通知', '本年度校级科研项目申报截止日期为6月30日。', 'academic', '2026-05-10'),
('校园科技文化节即将开幕', '5月20日-6月1日举办"创新·融合·未来"科技文化节。', 'activity', '2026-05-08'),
('2026年校园歌手大赛决赛', '决赛将于5月30日19:00在体育馆举行。', 'activity', '2026-05-15'),
('关于2026年端午节放假安排的通知', '6月8日-10日放假调休，共3天。', 'notice', '2026-05-10'),
('图书馆端午节开放时间调整', '节日期间主馆1楼自习室24小时开放。', 'notice', '2026-05-10');

-- 插入空闲教室数据
INSERT INTO classroom (building, room_number, capacity, has_projector, has_ac, is_free) VALUES
('教学楼', '101', 60, 1, 1, 1),
('教学楼', '103', 60, 1, 1, 1),
('教学楼', '105', 60, 1, 1, 1),
('教学楼', '207', 40, 1, 1, 1),
('教学楼', '209', 40, 1, 1, 1),
('教学楼', '301', 80, 1, 1, 1),
('教学楼', '305', 80, 1, 1, 1),
('教学楼', '402', 60, 1, 1, 1),
('教学楼', '408', 60, 0, 1, 1),
('实验楼', '201', 40, 1, 0, 1),
('实验楼', '203', 40, 1, 0, 1),
('实验楼', '301', 60, 1, 1, 1),
('实验楼', '304', 40, 0, 1, 1),
('实验楼', '401', 40, 1, 1, 1),
('实验楼', '403', 40, 0, 1, 1),
('逸夫楼', '102', 30, 1, 1, 1),
('逸夫楼', '104', 30, 1, 1, 1),
('逸夫楼', '106', 30, 0, 1, 1),
('逸夫楼', '201', 50, 1, 1, 1),
('逸夫楼', '203', 50, 1, 1, 1),
('逸夫楼', '302', 80, 1, 1, 1),
('逸夫楼', '305', 80, 1, 1, 1);
