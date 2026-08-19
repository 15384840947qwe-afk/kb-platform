-- ============================================================
-- KB 知识库演示种子数据（幂等：INSERT IGNORE，可反复执行）
-- 用法：kb-deploy\seed.ps1
-- 账号：zhangsan / lisi / wangwu / zhaoliu / chenhao，密码均为 123456
-- admin 由 AdminSeeder 启动播种，不在这里
-- ============================================================
USE kb;
SET NAMES utf8mb4;

-- ===== 用户（id从100起，避开自增区） =====
INSERT IGNORE INTO t_user (id, username, password, nickname, role, status) VALUES
(100, 'zhangsan', '$2a$10$oOZKPHU/ZdOj4egD.VlKxuBZf4j.qKZCyhKDRcYCblKKe.Kh6HjCO', '张三', 'MEMBER', 1),
(101, 'lisi',     '$2a$10$oOZKPHU/ZdOj4egD.VlKxuBZf4j.qKZCyhKDRcYCblKKe.Kh6HjCO', '李四', 'MEMBER', 1),
(102, 'wangwu',   '$2a$10$oOZKPHU/ZdOj4egD.VlKxuBZf4j.qKZCyhKDRcYCblKKe.Kh6HjCO', '王五', 'MEMBER', 1),
(103, 'zhaoliu',  '$2a$10$oOZKPHU/ZdOj4egD.VlKxuBZf4j.qKZCyhKDRcYCblKKe.Kh6HjCO', '赵六', 'MEMBER', 1),
(104, 'chenhao',  '$2a$10$oOZKPHU/ZdOj4egD.VlKxuBZf4j.qKZCyhKDRcYCblKKe.Kh6HjCO', '陈浩', 'MEMBER', 1);

-- ===== 题库：3科目×6题（id从200起） =====
-- Java基础
INSERT IGNORE INTO t_question (id, category, type, stem, options, answer, explanation) VALUES
(200, 'Java基础', 'SINGLE', '下列关于Java中String的说法，正确的是？',
 '["String是可变的","String的内容修改后原对象会改变","String是不可变类，内容不可修改","String不是final类"]',
 'C', 'String内部由final char数组支撑，任何"修改"都产生新对象，原对象内容不变。'),
(201, 'Java基础', 'SINGLE', 'HashMap的默认初始容量是多少？',
 '["8","16","32","64"]',
 'B', 'HashMap默认初始容量16，负载因子0.75，扩容为原容量2倍。'),
(202, 'Java基础', 'SINGLE', '下列哪个关键字用于保证变量的可见性？',
 '["static","volatile","transient","synchronized"]',
 'B', 'volatile保证多线程间的可见性并禁止指令重排，但不保证原子性。'),
(203, 'Java基础', 'MULTI', '下列哪些属于面向对象的特性？',
 '["封装","继承","多态","递归"]',
 'ABC', '面向对象三大特性：封装、继承、多态；递归是一种编程技巧。'),
(204, 'Java基础', 'FILL', 'Java中所有类的顶级父类是____。', NULL,
 'Object', 'java.lang.Object是所有类的根父类，提供equals、hashCode、toString等方法。'),
(205, 'Java基础', 'SHORT', '请简述Java中垃圾回收（GC）的基本原理。', NULL,
 '通过可达性分析找出不可达对象进行回收。从GC Roots出发遍历引用链，遍历不到的对象视为可回收；回收采用分代收集策略，新生代用复制算法，老年代用标记-整理算法。',
 '考察JVM基础：可达性分析、GC Roots、分代收集是答题要点。'),

-- MySQL
(206, 'MySQL', 'SINGLE', 'InnoDB引擎的默认事务隔离级别是？',
 '["读未提交","读已提交","可重复读","串行化"]',
 'C', 'InnoDB默认隔离级别为可重复读（RR），通过MVCC+间隙锁基本解决幻读。'),
(207, 'MySQL', 'SINGLE', '下列哪种索引结构是InnoDB默认使用的？',
 '["哈希索引","B+树索引","红黑树索引","跳表索引"]',
 'B', 'InnoDB使用B+树组织索引，叶子节点存数据并通过双向链表相连，利于范围查询。'),
(208, 'MySQL', 'SINGLE', '关于联合索引(a,b,c)，下列哪个查询能用到索引？',
 '["WHERE b=1","WHERE a=1 AND c=3","WHERE c=3","WHERE a=1 AND b=2 AND c=3"]',
 'D', '最左前缀原则：联合索引必须从最左列开始连续使用，a=1 AND b=2 AND c=3完整命中。'),
(209, 'MySQL', 'MULTI', '下列哪些操作会导致索引失效？',
 '["对索引列使用函数","隐式类型转换","LIKE ''%abc''前缀模糊","使用覆盖索引"]',
 'ABC', '函数运算、隐式转换、前缀模糊都会让优化器放弃索引；覆盖索引反而是优化手段。'),
(210, 'MySQL', 'FILL', 'MySQL中查看SQL执行计划的命令是____。', NULL,
 'EXPLAIN', 'EXPLAIN放在SQL前可查看执行计划，重点关注type、key、rows、Extra字段。'),
(211, 'MySQL', 'SHORT', '请简述事务的ACID特性。', NULL,
 '原子性(Atomicity)：事务要么全做要么全不做；一致性(Consistency)：事务前后数据满足约束；隔离性(Isolation)：并发事务互不干扰；持久性(Durability)：提交后修改永久生效。',
 'ACID是事务理论基石，能结合redo log/undo log/锁机制展开更好。'),

-- Redis
(212, 'Redis', 'SINGLE', 'Redis默认使用哪个数据库编号？',
 '["db0","db1","db15","无默认"]',
 'A', 'Redis默认有16个数据库（db0~db15），连接后默认使用db0。'),
(213, 'Redis', 'SINGLE', '下列哪个命令用于设置键的过期时间？',
 '["SET","EXPIRE","PERSIST","TTL"]',
 'B', 'EXPIRE设置秒级过期时间，PEXPIRE毫秒级；PERSIST移除过期时间，TTL查看剩余时间。'),
(214, 'Redis', 'SINGLE', '关于Redis的单线程模型，下列说法正确的是？',
 '["Redis所有操作都用多线程","命令执行单线程，6.0后网络IO多线程","Redis完全不支持并发","单线程导致Redis性能很差"]',
 'B', 'Redis命令执行是单线程避免锁竞争，6.0引入多线程处理网络IO提升吞吐。'),
(215, 'Redis', 'MULTI', 'Redis支持的数据类型包括？',
 '["String","Hash","List","B+树"]',
 'ABC', 'Redis五大基础类型：String、Hash、List、Set、ZSet；B+树是MySQL索引结构。'),
(216, 'Redis', 'FILL', 'Redis的持久化方式有RDB和____两种。', NULL,
 'AOF', 'RDB是内存快照，恢复快；AOF记录写命令日志，数据更安全，两者可结合使用。'),
(217, 'Redis', 'SHORT', '请简述什么是缓存穿透，以及如何解决。', NULL,
 '缓存穿透指查询根本不存在的数据，缓存和数据库都查不到，请求全部打到数据库。解决方案：缓存空值（设置短过期时间）、布隆过滤器拦截非法请求、参数合法性校验。',
 '考察缓存经典问题，能区分穿透/击穿/雪崩并分别给出方案是加分项。');

-- ===== 刷题记录 =====
-- admin(1)：MySQL强(8对2错)、Java中等(5对5错)、Redis薄弱(1对4错)——演示薄弱点推题
INSERT IGNORE INTO t_practice (user_id, question_id, result, create_time) VALUES
(1, 206, 1, NOW() - INTERVAL 6 DAY), (1, 207, 1, NOW() - INTERVAL 6 DAY),
(1, 208, 0, NOW() - INTERVAL 5 DAY), (1, 208, 1, NOW() - INTERVAL 4 DAY),
(1, 209, 1, NOW() - INTERVAL 4 DAY), (1, 210, 1, NOW() - INTERVAL 3 DAY),
(1, 211, 1, NOW() - INTERVAL 3 DAY), (1, 206, 1, NOW() - INTERVAL 2 DAY),
(1, 209, 0, NOW() - INTERVAL 2 DAY), (1, 210, 1, NOW() - INTERVAL 1 DAY),
(1, 200, 1, NOW() - INTERVAL 6 DAY), (1, 201, 0, NOW() - INTERVAL 6 DAY),
(1, 202, 1, NOW() - INTERVAL 5 DAY), (1, 203, 0, NOW() - INTERVAL 5 DAY),
(1, 204, 1, NOW() - INTERVAL 4 DAY), (1, 205, 0, NOW() - INTERVAL 4 DAY),
(1, 200, 1, NOW() - INTERVAL 3 DAY), (1, 203, 1, NOW() - INTERVAL 2 DAY),
(1, 205, 0, NOW() - INTERVAL 1 DAY), (1, 204, 1, NOW() - INTERVAL 1 DAY),
(1, 212, 1, NOW() - INTERVAL 5 DAY), (1, 213, 0, NOW() - INTERVAL 4 DAY),
(1, 214, 0, NOW() - INTERVAL 3 DAY), (1, 215, 0, NOW() - INTERVAL 2 DAY),
(1, 217, 0, NOW() - INTERVAL 1 DAY),
-- zhangsan(100)：均衡练习
(100, 200, 1, NOW() - INTERVAL 5 DAY), (100, 201, 1, NOW() - INTERVAL 4 DAY),
(100, 206, 0, NOW() - INTERVAL 3 DAY), (100, 212, 1, NOW() - INTERVAL 2 DAY),
(100, 208, 1, NOW() - INTERVAL 1 DAY),
-- lisi(101)：偏科Java
(101, 200, 1, NOW() - INTERVAL 6 DAY), (101, 201, 1, NOW() - INTERVAL 5 DAY),
(101, 202, 1, NOW() - INTERVAL 4 DAY), (101, 203, 0, NOW() - INTERVAL 3 DAY),
(101, 213, 0, NOW() - INTERVAL 2 DAY),
-- wangwu(102)：少量练习
(102, 206, 1, NOW() - INTERVAL 3 DAY), (102, 209, 0, NOW() - INTERVAL 2 DAY),
(102, 214, 0, NOW() - INTERVAL 1 DAY),
-- zhaoliu(103) / chenhao(104)：各两条
(103, 200, 0, NOW() - INTERVAL 2 DAY), (103, 212, 1, NOW() - INTERVAL 1 DAY),
(104, 207, 1, NOW() - INTERVAL 2 DAY), (104, 215, 0, NOW() - INTERVAL 1 DAY);

-- ===== 模拟面试记录（transcript/report给最小合法JSON） =====
INSERT IGNORE INTO t_interview (user_id, category, score, transcript, report, create_time) VALUES
(1, 'MySQL', 72, '[]', '{"summary":"基础扎实，索引优化细节可再深入"}', NOW() - INTERVAL 5 DAY),
(1, 'Java基础', 68, '[]', '{"summary":"集合与并发有基础，JVM部分偏弱"}', NOW() - INTERVAL 3 DAY),
(1, 'Redis', 55, '[]', '{"summary":"缓存基础概念需要系统补齐"}', NOW() - INTERVAL 1 DAY),
(100, 'Java基础', 78, '[]', '{"summary":"表达清晰，项目经验丰富"}', NOW() - INTERVAL 4 DAY),
(101, 'MySQL', 82, '[]', '{"summary":"事务与索引掌握较好"}', NOW() - INTERVAL 2 DAY),
(102, 'Redis', 61, '[]', '{"summary":"了解基本数据结构，持久化方案需补充"}', NOW() - INTERVAL 1 DAY);
