## 总体架构
- 网关与编排：使用 `Spring Cloud Gateway` 统一入口；`Orchestrator` 编排意图识别、路由、调用各能力服务；SSE流式返回。
- 能力服务：
  - `intent-service`（意图识别与问题分类）
  - `qa-standard-service`（标准问题库命中）
  - `category-router-service`（商品类目智能路由）
  - `product-reco-service`（类目选型与推荐，含品牌维度拆分）
  - `competitor-replacement-service`（竞品替换规则判断）
  - `search-service`（关键词搜索Top3）
  - `formatter-agent`（统一结果格式化）
  - `knowledge-service`（RAG/知识库/图谱接入与整合）
  - `tracking-service`（营销埋点与链接生成）
- 数据层：`MySQL` 业务数据与长期记忆元信息；可选 `Redis/Caffeine` 缓存；向 `Dify` 内置向量库或外部向量库托管检索。
- 大模型：统一接入 `Dify`，模型提供方为豆包；多模态（文本+图片URL/Base64）。
- 观测与治理：`Resilience4j` 熔断/限流/超时；`Micrometer` + `Prometheus/Grafana` 监控；结构化日志。

## 在线请求数据流
- 步骤：
  1. `Intent`：判定是否导购、是否标准问题、类目、是否需要品牌粒度拆分、关键词
  2. `Standard QA`：若命中标准问题直接返回答案
  3. `Category Route`：根据类目、品牌维度路由到选型引擎
  4. `Knowledge Enrichment`：通过 API/知识库/RAG/图谱/MCP/搜索补充商品信息
  5. `Reasoning`：结合 enriched data 由豆包推理得到推荐
  6. `Competitor Replacement`：如适用，按规则输出可替代型号
  7. `Search Top3`：若未命中确定目标商品，抽取关键词搜索返回Top3
  8. `Formatter`：统一格式与风格；生成带营销埋点的链接
  9. `SSE`：串流返回，先快后慢；心跳维持；最终汇总
- A2A策略：多Agent并发，先完成的先推送；最终收敛为一致结果并发送 `final` 事件。

## SSE与并发治理
- `SseEmitter` 超时时间设为 `180s`；注册到内存队列并每 `10s` 发送心跳消息。
- 网关返回头：`X-Accel-Buffering: no` 保持全链路长连接。
- `CompletableFuture`/`@Async` 并发；所有异步调用设置 `TimeLimiter`，超时降级；线程池参数明确与隔离。
- `Resilience4j`：重试、熔断、限流；失败快速返回通用文案。

## 缓存与“长期记忆”
- 缓存：标准问题、类目路由、热门商品列表、品牌维度拆分策略，使用 `Caffeine`（本地）+ 可选 `Redis`（共享）。
- 长期记忆框架（离线）：
  - 存储：高质量回答沉淀为标准问题或知识片段
  - 检索：查询标准问题与相关文档
  - 更新：定期评估质量与点击/转化指标
  - 融入交互：在 1.2 标准问题命中与 2.x 类目选型阶段优先使用

## API设计（对外）
- `POST /api/v1/guide/stream`（SSE）：请求体包含 `text`、`images[]`、`userId`、`sessionId`；事件类型：`intent`、`stdqa`、`route`、`enrich`、`reco`、`replacement`、`search`、`formatted`、`final`、`ping`；每条含 `traceId`。
- `GET /api/v1/recommendations/{requestId}`：查询最终推荐结果。
- `POST /api/v1/tracking`：接收点击/曝光等埋点事件。

## 统一返回格式（formatter-agent）
- 结构：
  - `title`：一句话结论
  - `summary`：2-3行理由
  - `items[]`：`{name, sku, brand, category, score, reason, price, stock, link}`
  - `alternatives[]`：竞品替换项，含 `sourceSku -> targetSku`
  - `trace`：关键决策与数据来源
  - `final`: 布尔，是否最终结果

## Doubao 提示词模板（Dify）
- 意图识别（System）：
  - 角色：严格的导购意图分类器，仅输出 JSON
  - 输入：用户文本、可选图片描述
  - 输出JSON：`{isGuide, isStandard, category, brandSplitNeeded, keywords[], confidence}`
- 标准问题（System）：
  - 角色：受限回答器，只能在给定标准问题库中匹配并返回答案或 `not_found`
  - 输入：用户问题、`standard_questions[]`
  - 输出：`{hit: boolean, questionId?, answer?}`
- 类目选型（System）：
  - 角色：工业品选型专家
  - 输入：用户需求、约束条件、已检索到商品数据（含品牌与关键规格）、可选图片特征
  - 输出：`top3` 候选与理由；若明确目标，`exact_target`。
- 竞品替换（System）：
  - 角色：替换规则裁判
  - 输入：厂家替换标准（结构化规则）、源/目标型号信息
  - 输出：`{replaceable: boolean, targetSku, reason, confidence}`
- 其它问题（System）：
  - 角色：工业领域顾问
  - 要求：先思考后输出，简洁可执行
- 输出格式化Agent（System）：
  - 角色：统一风格格式化器
  - 输入：前述阶段的结构化结果与数据来源
  - 输出：遵循“统一返回格式”结构的 JSON

## MySQL表结构（核心）
```sql
CREATE TABLE user_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  external_id VARCHAR(64) UNIQUE,
  name VARCHAR(128),
  segment VARCHAR(64),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES user_profile(id)
);

CREATE TABLE request_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  request_type VARCHAR(32),
  status VARCHAR(32),
  started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP NULL,
  latency_ms INT,
  error_code VARCHAR(32),
  FOREIGN KEY (session_id) REFERENCES session(id)
);

CREATE TABLE intent_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  is_guide TINYINT(1),
  is_standard TINYINT(1),
  category VARCHAR(128),
  brand_split_needed TINYINT(1),
  keywords_json JSON,
  confidence DECIMAL(5,2),
  FOREIGN KEY (request_id) REFERENCES request_log(id)
);

CREATE TABLE standard_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(512),
  normalized_hash VARCHAR(64) UNIQUE,
  answer TEXT,
  category VARCHAR(128),
  enabled TINYINT(1) DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) UNIQUE,
  parent_id BIGINT NULL
);

CREATE TABLE brand (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) UNIQUE
);

CREATE TABLE product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NOT NULL,
  brand_id BIGINT NOT NULL,
  sku VARCHAR(128) UNIQUE,
  name VARCHAR(256),
  spec_json JSON,
  price DECIMAL(12,2),
  stock INT,
  url VARCHAR(512),
  enabled TINYINT(1) DEFAULT 1,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (category_id) REFERENCES product_category(id),
  FOREIGN KEY (brand_id) REFERENCES brand(id)
);

CREATE TABLE competitor_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source_brand VARCHAR(128),
  source_sku VARCHAR(128),
  target_brand VARCHAR(128),
  target_sku VARCHAR(128),
  rule_json JSON,
  confidence DECIMAL(5,2),
  enabled TINYINT(1) DEFAULT 1,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE recommendation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  product_id BIGINT,
  rank INT,
  reason TEXT,
  score DECIMAL(6,3),
  link_url VARCHAR(512),
  FOREIGN KEY (request_id) REFERENCES request_log(id),
  FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE tracking_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rec_id BIGINT,
  user_id BIGINT,
  session_id BIGINT,
  event_type VARCHAR(64),
  product_id BIGINT,
  url VARCHAR(512),
  ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  metadata_json JSON
);

CREATE TABLE long_memory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  key_name VARCHAR(256) UNIQUE,
  type VARCHAR(64),
  content TEXT,
  embedding_id VARCHAR(128),
  source VARCHAR(64),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE prompt_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128),
  version VARCHAR(32),
  role VARCHAR(32),
  content TEXT,
  enabled TINYINT(1) DEFAULT 1,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE llm_call_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT,
  model VARCHAR(64),
  prompt_template_id BIGINT,
  success TINYINT(1),
  latency_ms INT,
  tokens_input INT,
  tokens_output INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 类目选型与品牌拆分策略
- 类目配置包含：是否需要品牌维度拆分、优先品牌列表、关键规格字段清单、选型打分公式。
- 策略可缓存并允许动态更新；对热门品牌/型号优先返回。

## 搜索Top3接口
- 输入：关键词（由模型抽取）、可选类目/品牌过滤；分页与排序。
- 输出：Top3商品列表与链接；若无结果，返回扩展词建议。

## 埋点与链接生成
- 链接格式：在商品 `url` 追加 `utm_source=ai_guide&utm_campaign=<scene>&rec_id=<id>&user=<uid>`；所有点击/曝光事件写入 `tracking_event`。

## 知识补充通道
- API：调用厂商/电商/内部ERP/PLM接口
- 知识库：Dify 的 KB；离线定期导入文档与结构化表格
- 图谱：若已有工业件图谱，提供 `MCP` 工具供模型检索

## Dify 工作流集成
- 一个编排工作流：节点包含意图->标准问答->路由->选型->替换->搜索->格式化；对每节点设置超时与失败回退。
- 工具节点：HTTP 请求工具（搜索/ERP）、KB检索工具、图谱查询工具。
- 模型统一选择豆包；多模态输入通过图片URL传入并由前置工具提取摘要特征。

## 并发与超时细则
- 线程池：每能力服务单独线程池，设定核心/最大、队列类型、拒绝策略。
- 超时：`TimeLimiter` 配置如 `intent 2s / stdqa 1s / enrich 3s / reco 3s / replacement 2s / search 2s / formatter 1s`。
- 降级：触发超时/熔断时返回通用文案与可用的Top3搜索。

## 监控与质量评估
- 指标：意图命中率、标准问答命中率、推荐点击率、转化率、时延分布、各节点错误率。
- 评估：高质量回答自动入库至 `standard_question`，低质量标记人工复核。

## 交付里程碑
- M1：骨架项目+网关+SSE通路+Dify工作流打通
- M2：意图/标准问答/类目路由与选型初版+MySQL建表
- M3：竞品替换与搜索Top3+统一格式化+埋点
- M4：缓存与长期记忆闭环+观测与治理完善
- M5：性能压测与高并发优化、故障演练

## 验证与测试
- 单元测试：意图分类、路由、打分、替换规则
- 集成测试：SSE流式完整链路、超时与降级
- 回归测试：标准问题库命中与格式化一致性

## 安全与合规
- 秘钥管理：模型/Dify/外部API密钥使用配置中心或KMS；不入库
- 图片与PII：脱敏与有效期；仅保存必要元数据
- 访问控制：各服务JWT鉴权与网关限流

## 需要确认
- 是否已有品牌/类目词典与选型规格清单
- 外部数据源可用范围（ERP/PLM/电商/厂商）
- Dify实例与豆包模型配额
- 是否启用Redis与是否需要外部向量库
