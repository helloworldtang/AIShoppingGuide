# AI导购系统实施方案（SpringBoot+Spring Cloud+Dify+豆包）

## 总体架构
- 网关与编排：`Spring Cloud Gateway` 统一入口；`Orchestrator` 编排意图识别、路由、调用各能力服务；SSE 流式返回。
- 能力服务：意图、标准问答、类目路由、选型、竞品替换、搜索、格式化、知识补充、埋点。
- 数据层：MySQL 与缓存；长期记忆与向量检索由 Dify/外部向量库托管。
- 大模型：统一接入 Dify，模型提供方为豆包；多模态（文本+图片）。
- 观测与治理：Resilience4j 熔断/限流/超时；监控与结构化日志。

## 在线请求数据流
1. 意图：是否导购、是否标准问题、类目与品牌拆分、关键词
2. 标准问答：命中直接返回
3. 类目路由：路由到选型引擎
4. 知识补充：API/知识库/RAG/图谱/MCP/搜索
5. 推理推荐：结合数据由豆包输出推荐
6. 竞品替换：按厂家规则判断
7. 搜索 Top3：未命中确定目标时回退搜索
8. 格式化：统一风格，生成带埋点链接
9. SSE：先快先发，最终 `final` 事件

## SSE 与并发治理
- 超时：`SseEmitter` 180s；心跳每 10s。
- 并发：`CompletableFuture/@Async`；所有异步设置 `TimeLimiter` 超时与降级。
- Resilience4j：重试/熔断/限流；失败快速返回通用文案。

## 缓存与长期记忆
- 缓存：标准问题、路由策略、热门商品与品牌拆分策略。
- 长期记忆：高质量回答沉淀，定期评估与更新，融入 1.2 与 2.x 阶段。

## 对外 API
- `POST /api/v1/guide/stream`（SSE）
- `GET /api/v1/recommendations/{requestId}`
- `POST /api/v1/tracking`

## 统一返回格式
- `title`、`summary`、`items[]`、`alternatives[]`、`trace`、`final`

## Doubao 提示词模板（Dify）
- 意图、标准问答、类目选型、竞品替换、其它问题、格式化 Agent。

## MySQL 表结构
- 位置：`sql/schema.sql`
- 覆盖：用户/会话、请求与意图、标准问答、商品与类目、替换规则、推荐与埋点、长期记忆、提示词模板与 LLM 调用日志。

## 类目与品牌策略
- 是否品牌维度拆分、关键规格字段清单、选型打分公式；支持缓存与动态更新。

## 搜索 Top3
- 输入关键词与类目/品牌过滤；输出 Top3 与链接；无结果返回扩展词建议。

## 埋点与链接
- 在商品 `url` 追加 `utm_source=ai_guide&utm_campaign=<scene>&rec_id=<id>&user=<uid>`；事件写入 `tracking_event`。

## Dify 工作流
- 编排节点：意图→标准问答→路由→选型→替换→搜索→格式化；设置超时与失败回退。

## 并发与超时细则
- 线程池隔离；意图 2s / 标准问答 1s / enrich 3s / reco 3s / replacement 2s / search 2s / formatter 1s。

## 监控与评估
- 指标：命中率、点击/转化、时延分布与错误率；高质量回答自动入库，低质量人工复核。

## 里程碑
- M1 骨架与SSE通路
- M2 意图/标准问答/路由/选型+建表
- M3 替换/搜索/格式化/埋点
- M4 缓存与长期记忆闭环
- M5 性能压测与并发优化

## 安全与合规
- 秘钥在配置中心或 KMS；图片/PII 脱敏与有效期；JWT 鉴权与限流。
