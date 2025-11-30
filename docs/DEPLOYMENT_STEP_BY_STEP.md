# 部署与上线（Step by Step）

本指南面向入门者，覆盖两种运行模式、一键构建、数据库初始化与验证方法。

## 前置要求
- JDK 17、Maven 3.8+
- 可选：MySQL 8（如启用数据层）

## 1. 获取代码并构建
```
git clone <your_repo_url>
cd AIShoppingGuide
mvn -q -T 1C -DskipTests package
```

## 2. 选择运行模式
- 微服务模式：适合分工与扩展
  - 进入 `orchestrator` 启动（端口 8081）
    - 命令：`cd orchestrator && mvn spring-boot:run`
    - 或运行 JAR：`java -jar orchestrator/target/orchestrator-0.1.0-SNAPSHOT.jar`
  - 进入 `gateway` 启动（端口 8080）
    - 命令：`cd gateway && mvn spring-boot:run`
    - 或运行 JAR：`java -jar gateway/target/gateway-0.1.0-SNAPSHOT.jar`
  - 其他能力服务按需启动（占位 API 已可用）
- 一体化模式：同进程内聚，部署更简单
  - 进入 `all-in-one` 启动（端口 8081）
    - 命令：`cd all-in-one && mvn spring-boot:run`
    - 或运行 JAR：`java -jar all-in-one/target/all-in-one-0.1.0-SNAPSHOT.jar`
  - 进入 `gateway` 启动（端口 8080）
    - 命令：`cd gateway && mvn spring-boot:run`
    - 或运行 JAR：`java -jar gateway/target/gateway-0.1.0-SNAPSHOT.jar`
- 注意：`orchestrator` 与 `all-in-one` 端口相同，请同一环境仅启动其一。

## 3. 数据库初始化（可选）
```
mysql -u root -p -e "CREATE DATABASE aishoppingguide DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p aishoppingguide < sql/schema.sql
```
- 表结构说明见 `sql/schema.sql`

## 4. 启用数据源（可选）
- 默认不强制依赖数据库，可在需要的服务启用本地配置：
  - 新建 `application-local.yml`（示例）
```
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aishoppingguide?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```
- 运行服务时指定：`--spring.profiles.active=local`
 - 端口覆盖（示例）：`--server.port=9090`

## 5. 验证 SSE 接口
```
POST http://localhost:8080/api/v1/guide/stream
Body:
{
  "text": "我需要选一款工业传感器",
  "images": [],
  "userId": "u123",
  "sessionId": "s123"
}
```
- 预期：收到 `ping` 心跳与阶段事件，最终 `final`

## 6. 集成 Dify 与豆包（可选）
- 导入 `prompts/` 下模板到 Dify 工作流
- 在编排链路配置：意图→标准问答→路由→选型→替换→搜索→格式化，设置节点超时与降级
- 配置模型秘钥在安全的配置中心，勿入库

## 7. 观测与优化建议
- 并发设置：`orchestrator` 线程池（核心16/最大64/队列500），Tomcat 并发参数
- 超时治理：意图 2s、标准问答 1s、知识补充 3s、选型 3s、替换 2s、搜索 2s、格式化 1s
- 压测：使用 `perf/jmeter/guide_stream.jmx` 验证流式与降级策略

## 8. 常见故障定位
- SSE 无推送：检查网关与 `X-Accel-Buffering: no`、浏览器 EventSource 支持
- 端口冲突：`orchestrator` 与 `all-in-one` 二选一启动
- 数据库连接失败：检查 JDBC URL/账号、是否启用了 `local` profile

完成以上步骤即可在本地或单机完成上线与验证，如需生产部署，请结合公司基础设施进行服务编排、负载均衡与监控接入。
