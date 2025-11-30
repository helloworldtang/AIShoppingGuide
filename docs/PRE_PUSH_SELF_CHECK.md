# Push 前自查与验证（Step by Step）

本指南帮助在推送到 GitHub 前自查，避免低级编译/依赖/运行错误。可复制粘贴命令按序执行。

## 1. 构建与依赖
- 构建整个项目：
```
mvn -q -DskipTests package
```
- 如首次构建或 all-in-one 依赖未解析，安装到本地仓库：
```
mvn -q -DskipTests install
```
- 说明：父 POM 已固定 `spring-cloud-dependencies=2023.0.4` 且编译级别 `release=17`。

## 2. 服务运行（内聚 + 网关）
- 启动 all-in-one（8081）：
```
mvn -q -f all-in-one/pom.xml spring-boot:run
```
- 启动 gateway（使用 8090 端口，避免占用）：
```
mv
n -q -f gateway/pom.xml spring-boot:run -Dspring-boot.run.arguments="--server.port=8090"
```
- 提示：all-in-one 默认禁用 DataSource 自动配置（无需 DB 即可运行）。

## 3. 健康检查
```
curl -s http://localhost:8081/actuator/health
curl -s http://localhost:8090/actuator/health
```
- 预期：均返回 `{"status":"UP"}`。

## 4. SSE 接口验证
- 直接访问后端（推荐验证路径）：
```
curl -N --max-time 5 -H "Content-Type: application/json" \
  -d '{"text":"测试SSE","images":[],"userId":"u1","sessionId":"s1"}' \
  http://localhost:8081/api/v1/guide/stream
```
- 预期事件序列：`ping`、`intent`、`route`、`formatted`、`final`。
- 通过网关（当前占位实现在 Netty 下可能出现 `Empty reply from server`）：
```
curl -N --max-time 5 -H "Content-Type: application/json" \
  -d '{"text":"测试SSE","images":[],"userId":"u1","sessionId":"s1"}' \
  http://localhost:8090/api/v1/guide/stream
```
- 说明：若网关流式不稳定，请先以直连后端验证功能；网关 SSE 转发将作为后续增强。

## 5. 数据库初始化（可选）
```
mysql -u root -p -e "CREATE DATABASE aishoppingguide DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p aishoppingguide < sql/schema.sql
```
- 启用数据源示例（可选）：在需要的服务添加 `application-local.yml`，并以 `--spring.profiles.active=local` 启动。

## 6. 常见问题与修复
- 构建失败：检查 Maven 输出，确保父 POM版本与 JDK17；重新执行 `mvn -q -DskipTests install`。
- 端口冲突：`orchestrator` 与 `all-in-one` 均为 8081，请二选一启动；网关默认 8080，可使用 `--server.port=8090`。
- DataSource 错误：all-in-one 已禁用自动配置；如启用 DB，请正确配置 JDBC URL 与驱动。
- 网关 SSE “Empty reply”：直连后端验证；后续将提供网关 SSE 转发增强与 GET SSE 兼容路径。
- YAML 错误：避免重复顶级 key（如 `spring:`），参考 `gateway/src/main/resources/application.yml` 的结构。

## 7. 推送准备度自查
- 开源配套：
  - `LICENSE`（MIT）、`.gitignore`、`CONTRIBUTING.md`
  - CI：`.github/workflows/ci.yml`（JDK17 + Maven 构建）
- 文档：`README.md`、`docs/DEPLOYMENT_STEP_BY_STEP.md`、`docs/AI导购系统实施方案（SpringBoot+Spring Cloud+Dify+豆包）.md`
- 建议补强：最小示例数据与单元/集成测试、SSE 网关转发配置、发布脚本与版本标签。

## 8. 一键自查命令块（可复制）
```
# 构建与安装
mvn -q -DskipTests package && mvn -q -DskipTests install

# 启动服务
mvn -q -f all-in-one/pom.xml spring-boot:run &
mvn -q -f gateway/pom.xml spring-boot:run -Dspring-boot.run.arguments="--server.port=8090" &

# 健康检查
curl -s http://localhost:8081/actuator/health
curl -s http://localhost:8090/actuator/health

# SSE 验证（后端直连）
curl -N --max-time 5 -H "Content-Type: application/json" -d '{"text":"测试SSE","images":[],"userId":"u1","sessionId":"s1"}' http://localhost:8081/api/v1/guide/stream
```

完成以上步骤即可确认项目在本地可构建、可运行、接口可用，满足推送前的最小可用性验证。
