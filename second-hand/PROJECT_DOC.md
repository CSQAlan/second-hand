# 二手交易平台 — 项目全景文档

> 阅读本文档后，你将对整个项目的技术栈、架构设计、核心模块、高并发方案有一个完整的认知，然后按推荐顺序阅读源码。

---

## 一、项目一句话概括

这是一个 **全功能二手交易平台**，包含普通交易、限时秒杀、实时拍卖三大交易模式，并集成了 Elasticsearch 混合搜索、RAG 智能客服、Redis 缓存防护、Redisson 分布式锁、自定义 AOP 限流等企业级技术方案。

---

## 二、技术栈全景

### 后端 (Spring Boot 3.5 + Java 17)

| 层级 | 技术 | 说明 |
|------|------|------|
| **Web 框架** | Spring Boot 3.5.15 | RESTful API |
| **ORM** | MyBatis-Plus 3.5.7 | 零 SQL 基础 CRUD，11 个 Mapper 全部继承 `BaseMapper<T>` |
| **数据库** | MySQL 8.x | 11 张表，utf8mb4 编码 |
| **缓存** | Redis (Lettuce) | 商品缓存、秒杀库存、分布式锁、限流 |
| **分布式锁** | Redisson 3.31.0 | 拍卖出价互斥、缓存击穿防护 |
| **搜索引擎** | Elasticsearch 8.x | BM25 全文 + kNN 向量混合检索 |
| **向量嵌入** | Ollama (nomic-embed-text) | 768 维稠密向量，本地部署 |
| **安全框架** | Spring Security + JWT | 无状态认证，BCrypt 密码加密 |
| **AOP** | Spring AOP | 自定义 `@FastAuthorize` 注解 + Redis 滑动窗口限流 |
| **AI 客服** | DeepSeek API | RAG 架构：本地知识库 + 实时商品检索 + LLM 生成 |
| **异步任务** | Spring @Async + 线程池 | 秒杀订单异步创建 |
| **定时任务** | Spring @Scheduled | 拍卖自动结算、孤立文件清理 |
| **日志** | Logback + MDC TraceId | 链路追踪，滚动日志文件 |
| **工具** | Lombok | 减少样板代码 |

### 前端 (Vue 3 + Vite 8)

| 技术 | 说明 |
|------|------|
| Vue 3 (Composition API) | `<script setup>` 语法 |
| Vue Router 5 | 路由守卫 + 懒加载 |
| Pinia 3 | 用户状态、聊天状态管理 |
| Element Plus 2.14 | UI 组件库，暗色玻璃拟态主题 |
| Axios | HTTP 客户端 + 401/403 拦截器 |
| ECharts 6 | 管理后台数据可视化（折线/饼图/柱状图） |

### 基础设施

| 组件 | 端口 | 用途 |
|------|------|------|
| MySQL | 3306 | 主数据库 |
| Redis | 6379 | 缓存 + 分布式锁 + 限流 |
| Elasticsearch | 9200 | 搜索引擎 |
| Ollama | 11434 | 本地 Embedding 服务 |
| Spring Boot | 8080 | 后端 API |
| Vite Dev Server | 5173 | 前端开发 |

---

## 三、数据库设计 (11 张表)

```
┌─────────────────────────────────────────────────────────┐
│                     second_hand 数据库                    │
├──────────────┬──────────────────────────────────────────┤
│ user         │ 用户表 (id, username, password[BCrypt],   │
│              │ nickname, avatar, role[USER/ADMIN], ...)  │
├──────────────┼──────────────────────────────────────────┤
│ goods        │ 商品表 (id, name, price, category,        │
│              │ condition, status[在售/已下架/已售],        │
│              │ seller_id → user.id)                      │
├──────────────┼──────────────────────────────────────────┤
│ seckill_goods│ 秒杀商品 (id, goods_id → goods.id,        │
│              │ seckill_price, stock, start_time,         │
│              │ end_time)                                 │
├──────────────┼──────────────────────────────────────────┤
│ auction_goods│ 拍卖商品 (id, goods_id → goods.id,        │
│              │ start_price, current_price,               │
│              │ highest_bidder_id, min_increment,         │
│              │ start_time, end_time, status)             │
├──────────────┼──────────────────────────────────────────┤
│ orders       │ 订单表 (id, order_no, goods_id,           │
│              │ buyer_id, seller_id, price,               │
│              │ type[普通/秒杀/拍卖],                      │
│              │ status[待付款→已付款→已发货→已收货→         │
│              │       交易成功/已取消])                     │
├──────────────┼──────────────────────────────────────────┤
│ auction_record│ 出价记录 (id, auction_goods_id,          │
│              │ bidder_id, bid_price, bid_time)           │
├──────────────┼──────────────────────────────────────────┤
│ favorite     │ 收藏表 (user_id + goods_id 唯一联合索引)   │
├──────────────┼──────────────────────────────────────────┤
│ review       │ 评价表 (id, order_id, reviewer_id,        │
│              │ rating[1-5], type[买家评卖家/卖家评买家])   │
├──────────────┼──────────────────────────────────────────┤
│ chat_message │ 私信消息 (sender_id, receiver_id,         │
│              │ goods_id, content, is_read)               │
├──────────────┼──────────────────────────────────────────┤
│ knowledge_base│ RAG 知识库 (question, answer,            │
│              │ category[trade/seckill/auction/general])  │
├──────────────┼──────────────────────────────────────────┤
│ upload_file  │ 文件上传记录 (filename, file_path,         │
│              │ is_used, 用于定时清理孤立文件)              │
└──────────────┴──────────────────────────────────────────┘
```

**订单状态机**：
```
0-待付款 → 1-已付款(等待卖家发货) → 2-卖家已发货 → 3-交易成功
     ↘ 5-已取消
```

---

## 四、架构设计

### 4.1 分层架构

```
┌──────────────────────────────────────────────────────────────┐
│                        Vue 3 前端                             │
│  Layout → GoodsList / SeckillList / AuctionList / Dashboard  │
│  Components: ChatDialog, Chatbot(AI客服)                      │
│  Store: user(Pinia), chat(Pinia)                              │
└──────────────────────┬───────────────────────────────────────┘
                       │ HTTP (Axios) + JWT Bearer Token
                       ▼
┌──────────────────────────────────────────────────────────────┐
│                   Spring Security 过滤链                       │
│  CorsFilter → JwtAuthenticationFilter → UserContext(ThreaLocal)│
│  SecurityConfig: 放行公开接口, 其余需认证                       │
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│                   @FastAuthorize AOP 切面                      │
│  ① 登录校验 (UserContext)                                     │
│  ② Redis 滑动窗口限流 (ZSET + Lua 脚本)                       │
│  ③ IP 兜底限流 (未登录用户)                                    │
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│                    Controller 层 (12 个)                       │
│  AuthController, GoodsController, SeckillController,          │
│  AuctionController, OrderController, FavoriteController,      │
│  ReviewController, ChatController, ChatbotController,         │
│  AdminController, UserController, UploadController            │
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│                     Service 层 (11 个)                         │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────┐     │
│  │ GoodsService │  │SeckillService│  │ AuctionService   │     │
│  │ ·Redis 缓存  │  │·Lua 原子扣减  │  │·Redisson 分布式锁│     │
│  │ ·ES 混合搜索 │  │·异步创建订单  │  │·定时自动结算     │     │
│  │ ·延迟双删    │  │·乐观锁扣库存  │  │·Redis 缓存出价   │     │
│  └─────────────┘  └──────────────┘  └─────────────────┘     │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────┐     │
│  │ OrderService │  │ChatbotService│  │ElasticsearchSvc  │     │
│  │ ·订单状态机  │  │·RAG 检索     │  │·BM25+kNN 混合   │     │
│  │ ·事务管理    │  │·DeepSeek LLM │  │·IK 中文分词      │     │
│  └─────────────┘  └──────────────┘  └─────────────────┘     │
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│           Mapper 层 (11 个) + RedisCacheHelper                │
│  全部继承 MyBatis-Plus BaseMapper<T>，零 SQL 基础 CRUD        │
│  RedisCacheHelper: 防击穿(Redisson锁) / 防穿透(空值缓存)      │
│                    / 防雪崩(随机TTL偏移)                       │
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│  MySQL (11表)    │    Redis (缓存+锁+限流+秒杀库存)    │    ES  │
└──────────────────────────────────────────────────────────────┘
```

### 4.2 请求生命周期

```
HTTP 请求
  │
  ├─ TraceIdFilter: 生成/提取 X-Trace-Id → 写入 MDC (日志链路追踪)
  │
  ├─ CorsFilter: 处理跨域
  │
  ├─ JwtAuthenticationFilter:
  │   ├─ 提取 Authorization: Bearer <token>
  │   ├─ JwtUtils.validateToken() 验签
  │   ├─ 解析 userId/username/role → 写入 UserContext (ThreadLocal)
  │   └─ 设置 Spring Security Authentication
  │
  ├─ SecurityConfig.filterChain:
  │   ├─ 公开接口 → permitAll
  │   └─ 其余 → authenticated
  │
  ├─ @FastAuthorize AOP:
  │   ├─ 登录校验
  │   └─ Redis 滑动窗口限流
  │
  ├─ Controller → Service → Mapper → DB/Redis/ES
  │
  └─ GlobalExceptionHandler: 统一异常处理
      ├─ BusinessException → 业务错误码
      ├─ AccessDeniedException → 403
      └─ Exception → 500 系统错误
```

---

## 五、核心模块详解

### 5.1 秒杀系统 (Seckill) — 高并发核心

**架构：Redis Lua 原子操作 + 异步线程池 + 数据库乐观锁**

```
                    1000 并发请求
                         │
                         ▼
              ┌─────────────────────┐
              │   SeckillController  │
              │   POST /seckill/order│
              └──────────┬──────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │  SeckillService      │
              │  .executeSeckill()   │
              │                      │
              │  Redis Lua 脚本:     │
              │  ① SISMEMBER 防重复  │
              │  ② GET 检查库存      │
              │  ③ DECR 扣减库存     │
              │  ④ SADD 记录用户     │
              │  全部原子，返回结果码  │
              └──────────┬──────────┘
                    成功 │
                         ▼
              ┌─────────────────────┐
              │ @Async 异步线程池    │
              │ seckillExecutor      │
              │ (core=10, max=50,    │
              │  queue=1000)         │
              │                      │
              │ ① 乐观锁扣DB库存     │
              │    UPDATE ...         │
              │    WHERE stock > 0    │
              │ ② 创建订单           │
              │ ③ 更新商品状态       │
              └─────────────────────┘
```

**seckill.lua 脚本**：
```lua
-- KEYS[1] = 已购用户集合, KEYS[2] = 库存key
-- ARGV[1] = 用户ID
local duplicate = redis.call('SISMEMBER', KEYS[1], ARGV[1])
if duplicate == 1 then return -1 end          -- 重复购买

local stock = tonumber(redis.call('GET', KEYS[2]))
if stock == nil or stock <= 0 then return -2 end  -- 库存不足

redis.call('DECR', KEYS[2])                   -- 原子扣减
redis.call('SADD', KEYS[1], ARGV[1])          -- 记录用户
return 1                                       -- 成功
```

**为什么这样做？**
- **Redis Lua**：单线程串行执行，1000 个并发请求在 Redis 层排队，不会超卖
- **异步线程池**：Redis 扣减成功后，订单写入 MySQL 放到异步队列，用户立即得到响应
- **乐观锁**：`WHERE stock > 0` 兜底，即使 Redis 和 DB 短暂不一致也不会超卖
- **CallerRunsPolicy**：队列满时由调用线程同步执行，降级而不丢弃

---

### 5.2 拍卖系统 (Auction) — Redisson 分布式锁

**架构：Redisson RLock 互斥 + Redis 缓存当前最高价 + 定时自动结算**

```
用户 A 出价 100      用户 B 出价 120
     │                    │
     ▼                    ▼
┌──────────────────────────────────┐
│     AuctionService.placeBid()    │
│                                  │
│  lock = redisson.getLock(        │
│    "auction:lock:" + auctionId)  │
│                                  │
│  lock.lock(10s, TimeUnit.SEC)    │
│  ┌────────────────────────────┐  │
│  │ ① 校验拍卖状态和时间       │  │
│  │ ② Redis GET 当前最高价     │  │
│  │ ③ 校验出价 > 当前价+最小加价│  │
│  │ ④ UPDATE DB current_price  │  │
│  │ ⑤ UPDATE DB highest_bidder │  │
│  │ ⑥ Redis SET 新最高价       │  │
│  │ ⑦ INSERT 出价记录          │  │
│  └────────────────────────────┘  │
│  lock.unlock()                   │
└──────────────────────────────────┘

定时任务 (每 10 秒):
  AuctionService.closeEndedAuctions()
    ├─ 查询已到结束时间的拍卖
    ├─ 有最高出价者 → 自动生成订单 (status=待付款)
    └─ 无人出价 → 标记商品为已下架
```

**为什么用 Redisson 而不是 Redis SETNX？**
- Redisson 的 `RLock` 是可重入锁，支持看门狗自动续期
- `lock.tryLock(waitTime, leaseTime)` 可以设置等待超时，避免死锁
- 底层基于 Redis 的 Hash + Lua 脚本实现，支持集群模式
- 如果用原生 SETNX，你需要自己处理续期、可重入、释放时的原子性判断（只能释放自己加的锁）

---

### 5.3 Redis 缓存三大防护 (RedisCacheHelper)

```java
// 防击穿 (Cache Breakdown): 热点 key 过期时，大量请求同时穿透到 DB
getOrLoad(key, dbLoader, lockKey, ttlSeconds)
  ├─ 1. GET key → 命中直接返回
  ├─ 2. Redisson.getLock(lockKey).tryLock()
  │     └─ 获锁失败 → sleep(50ms) → 重试
  ├─ 3. Double-check: 再次 GET key (可能别的线程已加载)
  ├─ 4. dbLoader.load() → 从 DB 加载
  ├─ 5. SET key value (TTL + random 0~300s)  ← 防雪崩
  └─ 6. unlock

// 防穿透 (Cache Penetration): 查询不存在的数据，每次都打 DB
// → 缓存空值 placeholder，TTL = 2 分钟

// 防雪崩 (Cache Avalanche): 大量 key 同时过期
// → TTL = baseTTL + random(0, 300s)，分散过期时间

// 延迟双删 (Double-Delete): 更新 DB 后保证缓存一致性
updateDbAndEvictCache(key, dbUpdater)
  ├─ 1. dbUpdater.run() → 更新 DB
  ├─ 2. DEL key → 删除缓存
  └─ 3. 500ms 后再次 DEL key → 删除期间可能被旧数据回填的缓存
```

---

### 5.4 Elasticsearch 混合搜索

**架构：BM25 全文检索 + kNN 向量语义检索，双路召回融合排序**

```
用户搜索 "苹果手机"
       │
       ▼
┌──────────────────────────────────────────┐
│      ElasticsearchService.searchGoodsIds │
│                                          │
│  ① EmbeddingService.getEmbedding()       │
│     → 调用 Ollama nomic-embed-text       │
│     → 返回 768 维稠密向量                 │
│                                          │
│  ② 构建混合查询:                          │
│     {                                    │
│       "query": {                         │
│         "bool": {                        │
│           "must": [standard + IK 分词],   │
│           "filter": [category, status]    │
│         }                                │
│       },                                 │
│       "knn": {                           │
│         "field": "description_vector",   │
│         "query_vector": [768维向量],      │
│         "k": 10                          │
│       }                                  │
│     }                                    │
│                                          │
│  ③ BM25 分数 + kNN 分数 → 融合排序       │
│  ④ 返回商品 ID 列表                       │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  GoodsService 根据 ID 列表查询 MySQL      │
│  补全完整商品数据返回前端                   │
└──────────────────────────────────────────┘
```

**ES Index 映射设计**：
- `name`: text (IK 中文分词) + keyword
- `description`: text (standard 分词)
- `description_vector`: dense_vector (768 维, cosine 相似度)
- `category`, `status`, `price`: 精确匹配/过滤/排序字段

---

### 5.5 RAG 智能客服

```
用户提问: "秒杀怎么玩？"
       │
       ▼
┌──────────────────────────────────────────┐
│        ChatbotService.getChatResponse    │
│                                          │
│  ① 本地 RAG 检索 (knowledge_base 表)     │
│     → 按关键词匹配分类                    │
│     → 命中 → 拼接上下文                   │
│                                          │
│  ② 实时商品检索 (GoodsService.search)     │
│     → 用户问到具体商品时，搜索相关商品     │
│     → 拼接商品信息到 prompt               │
│                                          │
│  ③ 调用 DeepSeek LLM API                │
│     → System Prompt: 平台客服人设         │
│     → User Prompt: 检索上下文 + 用户问题   │
│     → 返回自然语言回答                    │
│                                          │
│  ④ 兜底: API 不可用时                    │
│     → 关键词匹配本地知识库                │
│     → 返回预设回答                       │
└──────────────────────────────────────────┘
```

---

### 5.6 安全体系

```
┌─────────────────────────────────────────────────────┐
│                   安全分层设计                        │
├─────────────────────────────────────────────────────┤
│ 第 1 层: Spring Security 过滤链                      │
│   · CSRF 禁用 (REST API 无需)                       │
│   · CORS 配置 (允许 localhost:*)                     │
│   · Session 无状态 (STATELESS)                      │
│   · URL 级别放行规则                                 │
├─────────────────────────────────────────────────────┤
│ 第 2 层: JWT 认证                                    │
│   · JwtAuthenticationFilter (OncePerRequest)        │
│   · HS256 签名, 24h 过期                             │
│   · Token 载荷: userId + username + role            │
│   · ThreadLocal UserContext 传递用户信息             │
├─────────────────────────────────────────────────────┤
│ 第 3 层: @FastAuthorize AOP                         │
│   · 方法级登录校验                                   │
│   · Redis 滑动窗口限流 (ZSET + Lua)                 │
│   · 未登录用户按 IP 限流                             │
├─────────────────────────────────────────────────────┤
│ 第 4 层: 业务权限                                    │
│   · 商品修改: 只有卖家本人或管理员                    │
│   · 订单操作: 只有买家/卖家                          │
│   · 评价权限: 订单状态 + 是否已评                    │
│   · 管理后台: ROLE_ADMIN 角色                       │
└─────────────────────────────────────────────────────┘
```

**限流 Lua 脚本 (滑动窗口)**：
```lua
-- ZSET 存储请求时间戳
-- ZREMRANGEBYSCORE 清除窗口外的记录
-- ZCARD 统计窗口内请求数
-- 超过 maxRequests → 拒绝
```

---

### 5.7 其他模块

| 模块 | 说明 |
|------|------|
| **收藏系统** | user_id + goods_id 联合唯一索引，Redis 缓存收藏数 |
| **评价系统** | 双向评价(买家评卖家/卖家评买家)，评分 1-5 星，自动完成订单 |
| **私信系统** | 轮询拉取(3秒)，未读消息计数，商品上下文关联 |
| **文件上传** | UUID 重命名，upload_file 表追踪，定时清理 1h 未使用的孤立文件 |
| **管理后台** | 用户/销售额/活跃商品统计，7 日交易趋势图，商品分类饼图，订单类型柱状图，操作日志监控 |
| **TraceId** | Servlet Filter 生成唯一追踪 ID，写入 MDC，贯穿整个请求链路的日志 |

---

## 六、源码阅读顺序推荐

### 第一阶段：骨架 (先跑起来知道长什么样)

| 序号 | 文件 | 为什么先看 |
|------|------|-----------|
| 1 | `SecondHandApplication.java` | 入口，3 个注解搞定一切 |
| 2 | `application.yml` | 全局配置，了解用了哪些中间件 |
| 3 | `schema.sql` | 数据库设计，11 张表的关系一目了然 |
| 4 | `common/Result.java` | 统一返回格式，所有接口都用它 |
| 5 | `config/SecurityConfig.java` | 安全配置，哪些接口放行一目了然 |

### 第二阶段：安全链路 (理解请求怎么进来的)

| 序号 | 文件 | 说明 |
|------|------|------|
| 6 | `security/JwtUtils.java` | JWT 签发和验证 |
| 7 | `security/JwtAuthenticationFilter.java` | 请求过滤，解析 Token |
| 8 | `security/UserContext.java` | ThreadLocal 用户上下文 |
| 9 | `annotation/FastAuthorize.java` | 自定义注解定义 |
| 10 | `aop/FastAuthorizeAspect.java` | AOP 切面，登录校验 + 限流 |
| 11 | `controller/AuthController.java` | 登录注册入口 |
| 12 | `service/AuthService.java` | 登录注册逻辑 |

### 第三阶段：核心业务 (商品 + 订单)

| 序号 | 文件 | 说明 |
|------|------|------|
| 13 | `entity/Goods.java` | 商品实体 |
| 14 | `entity/Order.java` | 订单实体，状态机 |
| 15 | `controller/GoodsController.java` | 商品接口 |
| 16 | `service/GoodsService.java` | ⭐ 商品服务，含 Redis 缓存 + ES 搜索 |
| 17 | `service/OrderService.java` | 订单服务，事务管理 |
| 18 | `util/RedisCacheHelper.java` | ⭐ 缓存三大防护工具类 |

### 第四阶段：高并发模块 (重点！)

| 序号 | 文件 | 说明 |
|------|------|------|
| 19 | `resources/lua/seckill.lua` | ⭐ 秒杀 Lua 脚本 |
| 20 | `config/ThreadPoolConfig.java` | 秒杀线程池配置 |
| 21 | `service/SeckillService.java` | ⭐ 秒杀服务：Lua + 异步 + 乐观锁 |
| 22 | `controller/SeckillController.java` | 秒杀接口 |
| 23 | `config/RedissonConfig.java` | Redisson 配置 |
| 24 | `service/AuctionService.java` | ⭐ 拍卖服务：分布式锁 + 定时结算 |
| 25 | `controller/AuctionController.java` | 拍卖接口 |

### 第五阶段：搜索 + AI

| 序号 | 文件 | 说明 |
|------|------|------|
| 26 | `service/EmbeddingService.java` | 向量嵌入服务 |
| 27 | `service/ElasticsearchService.java` | ⭐ ES 混合搜索 |
| 28 | `service/ChatbotService.java` | ⭐ RAG 智能客服 |
| 29 | `controller/ChatbotController.java` | AI 客服接口 |

### 第六阶段：其他模块

| 序号 | 文件 | 说明 |
|------|------|------|
| 30 | `service/FavoriteService.java` | 收藏 |
| 31 | `service/ReviewService.java` | 评价 |
| 32 | `service/UserService.java` | 用户信息 |
| 33 | `controller/ChatController.java` | 私信 |
| 34 | `controller/AdminController.java` | 管理后台 |
| 35 | `controller/OrderController.java` | 订单管理 |
| 36 | `filter/TraceIdFilter.java` | 链路追踪 |
| 37 | `scheduler/FileCleanScheduler.java` | 定时清理 |
| 38 | `exception/GlobalExceptionHandler.java` | 全局异常 |

### 第七阶段：前端 (可选)

| 序号 | 文件 | 说明 |
|------|------|------|
| 39 | `fronted/src/main.js` | 入口，Axios 拦截器 |
| 40 | `fronted/src/router/index.js` | 路由 + 守卫 |
| 41 | `fronted/src/store/user.js` | 用户状态 |
| 42 | `fronted/src/views/SeckillList.vue` | 秒杀前端，滑块验证 |
| 43 | `fronted/src/views/AuctionList.vue` | 拍卖前端 |
| 44 | `fronted/src/components/Chatbot.vue` | AI 客服组件 |

---

## 七、分布式集群 & 高并发在本项目中的应用 (深度解析)

### 7.1 什么是"分布式"在这个项目里的体现？

虽然当前部署是单机，但代码架构已经具备分布式能力：

| 能力 | 实现方式 | 如何扩展到集群 |
|------|---------|---------------|
| **无状态认证** | JWT Token，不依赖 Session | 多台后端实例共享同一个 JWT Secret 即可水平扩展 |
| **分布式锁** | Redisson RLock | Redisson 原生支持 Redis Cluster / Sentinel，改配置即可 |
| **分布式缓存** | Redis 单实例 | 切换为 Redis Cluster，Lettuce 客户端自动感知拓扑 |
| **分布式限流** | Redis ZSET + Lua | 多实例共享同一个 Redis，限流是全局的 |
| **分布式搜索** | Elasticsearch | ES 天然分布式，加节点即可扩容 |
| **异步解耦** | Spring @Async 线程池 | 生产环境可替换为 RabbitMQ/Kafka 消息队列 |

### 7.2 Redisson 在本项目的 3 个使用场景

**场景 1：拍卖出价互斥 (`AuctionService.placeBid`)**
```java
RLock lock = redissonClient.getLock("auction:lock:" + auctionGoodsId);
try {
    lock.lock(10, TimeUnit.SECONDS);
    // ① 校验拍卖状态
    // ② 校验出价 > 当前最高价 + 最小加价
    // ③ 更新 DB current_price, highest_bidder_id
    // ④ 更新 Redis 缓存
    // ⑤ 插入出价记录
} finally {
    lock.unlock();
}
```
- **为什么需要？** 两个用户同时出价，如果不加锁，可能同时读到相同的 currentPrice，都认为自己出价成功
- **为什么不用数据库行锁？** `SELECT ... FOR UPDATE` 在高并发下会锁表行，阻塞其他查询，性能差
- **Redisson 的优势**：锁粒度细(每个拍卖一把锁)，等待时间可控，自动续期

**场景 2：缓存击穿防护 (`RedisCacheHelper.getOrLoad`)**
```java
RLock lock = redissonClient.getLock(lockKey);
if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
    try {
        // Double-check: 再次查缓存
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) return deserialize(cached);
        // 从 DB 加载并写入缓存
        T result = dbLoader.load();
        redisTemplate.opsForValue().set(key, serialize(result), ttl, TimeUnit.SECONDS);
        return result;
    } finally {
        lock.unlock();
    }
} else {
    // 获取锁失败，短暂等待后重试查缓存
    Thread.sleep(50);
    return getFromCache(key);
}
```
- **为什么需要？** 商品详情是热点数据，如果缓存恰好过期，1000 个请求同时打到 DB
- **Redisson 的优势**：`tryLock` 非阻塞，获取失败的线程不会傻等，而是重试查缓存

**场景 3：限流（间接使用 Redis）**
```java
// FastAuthorizeAspect 中
String luaScript = """
    redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1])
    local count = redis.call('ZCARD', KEYS[1])
    if count < tonumber(ARGV[2]) then
        redis.call('ZADD', KEYS[1], ARGV[3], ARGV[3])
        redis.call('EXPIRE', KEYS[1], ARGV[4])
        return 1
    end
    return 0
    """;
```
- 滑动窗口限流：ZSET 存储每次请求的时间戳，窗口外的自动清除

### 7.3 高并发设计模式总结

```
┌─────────────────────────────────────────────────────────────┐
│                    高并发防护全景                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  请求入口                                                    │
│  ├── @FastAuthorize 滑动窗口限流 → 过载保护                   │
│  ├── Spring Security URL 放行 → 避免过滤器链瓶颈              │
│  │                                                          │
│  秒杀路径                                                    │
│  ├── Redis Lua 原子操作 → 库存扣减不超卖                     │
│  ├── SISMEMBER 去重 → 防止同一用户重复下单                    │
│  ├── @Async 线程池 → 订单写入异步化，快速响应                 │
│  ├── DB 乐观锁 (WHERE stock > 0) → 最终一致性兜底            │
│  └── CallerRunsPolicy → 队列满时降级不丢弃                   │
│                                                             │
│  拍卖路径                                                    │
│  ├── Redisson RLock → 出价互斥，防并发写                     │
│  ├── Redis 缓存当前最高价 → 减少 DB 查询                     │
│  └── @Scheduled 定时结算 → 自动关闭到期拍卖                  │
│                                                             │
│  商品查询                                                    │
│  ├── Redis 缓存 → 热点数据内存级响应                         │
│  ├── 防击穿: Redisson Lock + Double-Check                   │
│  ├── 防穿透: 空值缓存 (TTL=2min)                            │
│  ├── 防雪崩: 随机 TTL 偏移 (base + 0~300s)                  │
│  └── 延迟双删: 更新时先删缓存，500ms 后再删一次              │
│                                                             │
│  搜索                                                        │
│  ├── ES BM25 + kNN 混合 → 全文+语义双路召回                  │
│  └── 搜索结果只返回 ID → 再查 MySQL 补全，减少 ES 传输量     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 7.4 如果要做真正的集群部署，需要改什么？

| 改动点 | 当前 | 集群方案 |
|--------|------|---------|
| Redis | 单机 `localhost:6379` | Redis Cluster (6 节点) 或 Sentinel 哨兵 |
| Redisson | `useSingleServer()` | `useClusterServers()` 或 `useSentinelServers()` |
| MySQL | 单机 | 主从复制 + 读写分离 (MyBatis-Plus 动态数据源) |
| Elasticsearch | 单节点 | 多节点集群 (3 节点起) |
| 文件上传 | 本地 `uploads/` | MinIO / OSS 对象存储 |
| 异步任务 | Spring @Async 线程池 | RabbitMQ / Kafka 消息队列 |
| 会话 | JWT 无状态 | 无需改动 ✅ |
| 负载均衡 | 无 | Nginx 反向代理 + upstream |

---

## 八、项目亮点总结

1. **秒杀零超卖**：Redis Lua 原子脚本 + 异步线程池 + DB 乐观锁，三层防护
2. **拍卖强一致**：Redisson 分布式锁保证并发出价互斥
3. **缓存三防**：击穿(Redisson 锁) / 穿透(空值) / 雪崩(随机 TTL)，企业级方案
4. **搜索智能化**：ES BM25 + 向量 kNN 混合检索，支持语义搜索
5. **AI 客服 RAG**：本地知识库 + 实时商品数据 + DeepSeek LLM，三层增强
6. **安全分层**：Security 过滤链 → JWT → AOP 注解限流 → 业务权限，四层防护
7. **链路追踪**：TraceId Filter + MDC，日志中可追踪完整请求链路
8. **延迟双删**：更新 DB 后 500ms 再删一次缓存，保证最终一致性
9. **定时清理**：孤立上传文件自动清理，不浪费磁盘空间
10. **前端体验**：暗色玻璃拟态主题、滑块验证防机器人、实时轮询私信

---

> 📌 **建议**：按照第六节的阅读顺序，先跑通主流程(登录→浏览→购买)，再逐个深入高并发模块。秒杀和拍卖是整个项目技术含量最高的部分，值得反复研读。
