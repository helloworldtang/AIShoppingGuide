# AIShoppingGuide

AI 导购系统（SpringBoot + Spring Cloud + Dify + 豆包）。支持微服务运行模式与一体化（内聚）运行模式，面向提升转化率与人效，提供 SSE 流式返回、知识补充、选型推荐、竞品替换、搜索 Top3 与统一格式化输出。

## 1. 架构概览

- 入口与编排：`gateway` 作为统一入口；`orchestrator` 编排意图识别、路由、知识补充、选型、替换、搜索与格式化，SSE 流式返回。
- 能力服务：
  - `intent-service`：意图识别与问题分类
  - `qa-standard-service`：标准问题命中与缓存
  - `category-router-service`：商品类目智能路由与品牌拆分判定
  - `product-reco-service`：类目选型与推荐
  - `competitor-replacement-service`：竞品替换规则判断
  - `search-service`：关键词搜索 Top3
  - `formatter-agent`：统一风格格式化返回
  - `knowledge-service`：RAG/知识库/图谱/外部 API 整合
  - `tracking-service`：营销埋点与链接生成
- 数据层：MySQL（业务数据与长期记忆元数据），可选 Redis/Caffeine 缓存；向量检索由 Dify/外部向量库托管。
- 观测与治理：Resilience4j 超时/熔断/限流；心跳维持长连接；结构化日志与指标。

## 2. 项目结构

```
AIShoppingGuide/
├─ pom.xml                       # 父POM
├─ README.md                     # 项目说明（本文件）
├─ .gitignore                    # 忽略文件配置
├─ gateway/                      # Spring Cloud Gateway（8080）
├─ orchestrator/                 # 编排与SSE接口（8081）
├─ intent-service/
├─ qa-standard-service/
├─ category-router-service/
├─ product-reco-service/
├─ competitor-replacement-service/
├─ search-service/
├─ formatter-agent/
├─ knowledge-service/
├─ tracking-service/
├─ all-in-one/                   # 一体化（内聚）运行模式入口（8081）
├─ prompts/                      # 豆包提示词模板
├─ sql/schema.sql                # MySQL DDL
└─ perf/                         # 压测脚本与说明
```

## 3. 模块交互

- 网关将 `/api/**` 路由到编排服务（默认 `http://localhost:8081`）。
- 编排服务通过并发调用各能力服务（意图 → 标准问答 → 类目路由 → 知识补充 → 选型 → 替换 → 搜索 → 格式化），先快先发，最终事件 `final` 收敛。
- 埋点服务接收曝光/点击等事件，生成带 `utm` 的商品链接。
- SSE 接口事件类型：`intent`、`stdqa`、`route`、`enrich`、`reco`、`replacement`、`search`、`formatted`、`final`、`ping`。

## 4. 运行模式

- 微服务模式（默认）：分别启动各模块，服务间通过 HTTP 通信，适合团队分工与弹性扩展。
- 一体化（内聚）模式：只启动 `all-in-one` 与 `gateway`，`all-in-one` 扫描并加载所有能力服务的控制器，服务间走同进程调用（Spring Bean），降低跨进程通信开销。

## 5. 环境要求

- JDK 17
- Maven 3.8+
- MySQL 8（如启用数据层）

## 6. 快速开始

1. 构建

```
mvn -q -T 1C -DskipTests package
```

2. 运行（两种模式二选一）

- 微服务模式：
  - 启动编排：
    - 进入 `orchestrator` 运行应用（端口 8081）
  - 启动网关：
    - 进入 `gateway` 运行应用（端口 8080）
  - 其余能力服务可按需启动（占位 API 已可用）
- 一体化模式：
  - 启动 `all-in-one`（端口 8081）
  - 启动 `gateway`（端口 8080）

3. 数据库初始化（可选）

```
# 创建数据库
mysql -u root -p -e "CREATE DATABASE aishoppingguide DEFAULT CHARACTER SET utf8mb4;"

# 导入结构
mysql -u root -p aishoppingguide < sql/schema.sql
```

- 如需启用数据层，参考 `docs/DEPLOYMENT_STEP_BY_STEP.md` 配置数据源，并为相关服务启用 `local` 配置文件。

4. 测试 SSE 接口

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

- 浏览器或客户端应接收到 `ping` 心跳与阶段性事件，最终 `final`。

## 7. 数据库

- 初始化：执行 `sql/schema.sql`。
- 表用途：用户/会话、请求与意图、标准问答、商品与类目、替换规则、推荐与埋点、长期记忆、提示词模板与 LLM 调用日志。

## 8. 配置与治理

- 长连接：网关与编排统一返回 `X-Accel-Buffering: no`；SSE 超时 `180s`，每 `10s` 心跳。
- 并发：`orchestrator` 线程池（核心 16/最大 64/队列 500）；Tomcat 并发参数调整。
- 超时：意图 `2s`、标准问答 `1s`、知识补充 `3s`、选型 `3s`、替换 `2s`、搜索 `2s`、格式化 `1s`。
- 缓存：标准问答使用 Caffeine 缓存；可扩展 Redis 共享缓存。

## 9. 提示词模板

- 目录 `prompts/` 包含意图、标准问答、选型、替换、格式化的豆包模型模板，可直接导入 Dify 工作流使用。

## 10. 常见问题

- 端口冲突：`orchestrator` 与 `all-in-one` 都使用 8081，请二选一启动。
- SSE 未推送：检查网关是否启动、返回头是否包含 `X-Accel-Buffering: no`、浏览器是否支持 EventSource。
- 数据库不可用：先不启用数据层，待配置好 MySQL 后再开启。

## 11. 性能与内聚性分析

- 多项目调用瓶颈：在高并发场景下，跨进程 HTTP 调用会引入额外序列化/网络开销与调度延迟，尤其链路较长时更明显。
- 缓解措施：
  - 并发编排：分阶段并发调用，先快先发，减少排队时间
  - 超时与降级：对每阶段设置超时与回退，避免长尾阻塞
  - 缓存与命中：标准问答、热门商品、路由策略放入缓存，减少外呼
  - 连接池与线程池：合理配置最大并发，避免线程饥饿与队列堆积
- 内聚空间：
  - 引入 `all-in-one` 模式，将能力服务装载为同一 Spring 应用中的 Bean，减少跨进程 HTTP 带来的开销与故障面
  - 该模式不改变对外 API 与网关路由，部署更简单，适合单机/开发测试场景；生产可根据规模选择微服务或一体化
  - 注意：`orchestrator` 与 `all-in-one` 端口相同，请在同一环境下仅启动其一，避免端口冲突

## 12. 部署建议

- 开发/测试：推荐一体化模式（`all-in-one` + `gateway`）
- 生产初期：微服务模式，根据负载扩容关键能力服务；保留一体化模式作为快速回退方案
- 监控：接入 Prometheus/Grafana，关注时延分布、各阶段错误率、点击与转化指标

---

如需将 Dify 工作流与豆包模型真实接入到编排链路（含 RAG/外部 API），可依据 `prompts/` 模板与 `knowledge-service` 占位接口补齐调用逻辑，并在 `orchestrator` 加入工具节点调用与结果融合。
更多细节参见：

- `sql/schema.sql`（数据库脚本）
- `docs/DEPLOYMENT_STEP_BY_STEP.md`（小白上手部署指南）
- `docs/AI导购系统实施方案（SpringBoot+Spring Cloud+Dify+豆包）.md`（总体计划与技术方案）
- `docs/PRE_PUSH_SELF_CHECK.md`（Push 前自查与验证步骤）

## 13. 项目说明（做了什么 / 没做什么）

- 做了什么：
  - 多模块骨架、网关与编排、SSE 长连接与心跳、并发与超时治理
  - 能力服务占位 API：意图、标准问答、类目路由、选型、替换、搜索、格式化、知识补充、埋点
  - 数据库 DDL、提示词模板、部署与上线手册
  - 内聚运行模式（all-in-one），同进程降低开销
- 没做什么：
  - 未接入真实 Dify 工作流与豆包调用（当前为提示词与占位 API）
  - 未提供真实商品数据与选型打分公式（需按行业落地）
  - 未实现完整埋点落库与报表分析（接口已预留）

## 14. 技术难点与亮点

- 难点：
  - 高并发下的多阶段链路编排与超时/降级策略
  - SSE 全链路长连接在网关/反向代理环境的稳定性
  - 工业品选型与竞品替换规则的结构化建模
- 亮点：
  - 先快先发的流式编排，A2A 聚合最终一致
  - 内聚与微服务双模式切换，兼顾性能与团队协作
  - 统一格式化返回，便于前端接入与营销埋点
  - 规范化 DDL 与提示词模板，快速落地与扩展

## 15. 推送 GitHub 的准备度

- 已包含：`.gitignore`、`LICENSE`（MIT）、`CONTRIBUTING.md`、CI（GitHub Actions Maven 构建）
- 建议完善：
  - 新手示例请求与返回样例（可在 `docs/` 补充）
  - 更多单元/集成测试与示例数据
  - 发布脚本与版本标签管理
  - 安全配置示例（占位密钥的读取方式，不提交敏感信息）
