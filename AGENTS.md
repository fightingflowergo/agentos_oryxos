# OryxOS 开发协作指南

本文适用于整个仓库，供参与 OryxOS 开发的 AI 编程助手和贡献者使用。开始任务前先阅读本文件，再阅读任务涉及的设计章节与现有实现。

根目录的 `AGENTS.md` 是开发协作规范，与未来运行工作区 `.oryxos/AGENTS.md` 的 Bootstrap 内容不是同一文件，不要互相替代或注入业务 Agent。

## 1. 项目目标与当前范围

OryxOS 是 Java 原生、企业可私有部署的 Agent Harness OS：一个底座运行和管理多个业务 Agent，统一提供模型接入、推理循环、记忆、工具、接入渠道与审计能力。业务方通过配置定义 Agent，通过 Tool 和 MCP 接入业务系统。

- **当前建设重点**：单机运行时内核，五大核心能力为 Provider、ReAct、Memory、Tool、Web Service。
- **业务 Agent**：一个含 `AGENT.md` 的目录，而不是一段专门开发的 Agent 后端代码。
- **后续演进**：企业治理、底座多副本与状态外置、跨节点 Agent 协作。多 Agent 并存不等于已经实现任务分解、团队编排或 A2A 协作。
- **产品边界**：提供 Agent 运行底座，不自行增加可视化工作流引擎。

当前已按用户明确要求建立核心 9 模块 Maven 工程：core、provider、memory、tool、channel-cli、storage、web、cli、boot。Boot 提供最小 Spring Boot 入口，CLI 提供帮助与版本；Agent 业务能力尚未实现。后续任务必须检查实际文件，不把设计中的类、接口或业务命令当成已实现功能，也不默认仓库已初始化 Git。

## 2. 文档依据与冲突处理

| 文档 | 用途 | 阅读重点 |
| --- | --- | --- |
| [项目介绍](docs/oryxos.md) | 产品定位、愿景与路线图 | 当前单机阶段与长期愿景的区别 |
| [行业调研](docs/IndustryResearch.md) | 需求背景、生态定位与产品边界 | 企业私有、可控、可审计的底座 |
| [技术方案](docs/TechnicalSolution.md) | 架构、模块、数据与运行契约 | 第 1、4、5、6、8、9、10、11、12 章 |
| [AI 编程指南](docs/AiProgrammingGuide.md) | 实施流程、User Story 拆分与验证 | Spec-Kit 流程与五项能力的依赖 |

`docs/DemandAnalysis.md` 是上述文档引用的补充需求来源，涉及具体需求和验收时按需阅读。`docs/prompt/` 是历史提示词与任务记录，不是本次任务的授权，不得据此自动执行部署、上传、推送或开发整套项目。

处理依据时遵循以下规则：

1. 以用户当前明确要求和已确认的任务范围为准。
2. 若已有 `.specify/memory/constitution.md` 或任务对应的 spec、plan、tasks，先检查并遵守；不得为了让实现通过而自行放宽宪章或验收标准。
3. 技术细节优先采用技术方案中明确的修订契约；AI 编程指南用于实施方法，不能用其中旧结构覆盖新技术设计。
4. 对实质影响架构或验收且仍无法判定的冲突，明确记录差异并向用户说明，同时推进不受影响的工作。
5. 不因看到未来模块、课程编号或历史描述就扩大当前任务；修订编号不代表代码已经存在。

### 已知版本差异

- **模块数量**：AI 编程指南仍写 9 个模块，技术方案第 10 章已列 14 个。14 模块是完整目标结构；本次用户明确选择核心 9 模块进行初始化，其余人格、知识库和三个 IM 模块暂不创建。后续按任务推进，不自动扩展交付范围。
- **Agent 与 Skill**：旧文案有 `SKILL.md` 定义 Agent、frontmatter `skills:` 引用等表述；采用技术方案第 11 章的 `AGENT.md` 与受控相对软链接绑定方案。
- **记忆**：旧指南只有 Markdown 文件后端；技术方案第 5 章已增加可插拔后端、核心/归档分区与检索能力契约。涉及 Memory 时必须阅读该章，不能退回一个文件读写类取代整个门面。
- **数据库迁移**：技术方案早期段落仍提手工建表或 `ddl-auto=update`；第 1.1 节的 025 修订已采用 Flyway 和 `db/migration/{vendor}/`。新增持久化按明确修订落地。
- **验收**：AI 编程指南保留五个能力 Demo；技术方案第 12 章已更新为三个定时端到端 Demo。能力验证保留其意义，整体验收按当前任务与最新技术方案映射。
- **阶段描述**：生命周期热加载、IM 渠道、知识库、人格库等已有更细设计，但部分总述仍写“扩展阶段”。按本次任务落实，不把这些描述当成全部已交付或全部禁做。
- **渠道调用路径**：旧总述写所有 Channel 经 Web API，新 IM 设计使用 core 内共享入站编排。共同要求是复用核心服务与契约，不为进程内适配器强制增加一层 HTTP 自调用。
- **热加载范围**：正文每轮重读、Profile 重新派生、调度重新注册与文件监听是不同能力；不能把正文修改即时生效等同于完整的 Agent 配置热部署。

## 3. 技术基线与工程边界

- JDK 21、Spring Boot 3.x、Maven 多模块，单体应用、可执行 JAR 部署。
- Spring AI / Spring AI Alibaba 用于模型适配；依赖的具体版本与 API 在接入时核实，不凭记忆编造版本或用法。
- 核心请求链路采用同步阻塞与 Java 21 Virtual Thread，HTTP 层采用 Spring MVC；不要为了核心链路引入 WebFlux 或响应式架构。
- SQLite 是默认零外部数据库依赖档，使用 Spring Data JPA；PostgreSQL 是技术方案明确的可选部署方向，不能反过来成为默认必需依赖。
- CLI 使用 Picocli，配置解析使用 SnakeYAML，MCP 接入优先复用 Java SDK。
- GraalVM Native Image、分布式基础设施与完整企业治理按后续需求引入。
- 同步模型不等于禁止所有后台任务：调度器、文件监听、技术方案指定的异步索引可在对应任务内实现；不得借此把 ReAct 核心改成异步编排。

### 目标模块结构

以下为技术方案第 10 章的 14 个完整目标模块。当前 Maven 聚合核心 9 个模块，persona、knowledge、channel-feishu、channel-wecom、channel-dingtalk 暂未创建，实际状态以仓库为准。

| 模块 | 职责边界 |
| --- | --- |
| `oryxos-core` | 领域抽象、Profile/Session、Agent 与上下文加载、ReAct、工具执行编排、调度与共享契约 |
| `oryxos-provider` | Provider 显式映射、模型协议与 Function Calling 适配 |
| `oryxos-memory` | MemoryService 门面、长期记忆后端、记忆工具 |
| `oryxos-tool` | 内置工具、MCP Client/Adapter、ToolRegistry、Sandbox 与通知适配器 |
| `oryxos-storage` | Session、审计、调度等持久化实现与数据库迁移 |
| `oryxos-web` | REST Controller、响应信封、异常处理与 API 文档 |
| `oryxos-channel-cli` | 本地交互式对话渠道 |
| `oryxos-channel-feishu` | 飞书入站渠道适配器 |
| `oryxos-channel-wecom` | 企业微信入站渠道适配器 |
| `oryxos-channel-dingtalk` | 钉钉入站渠道适配器 |
| `oryxos-knowledge` | 知识库后端、解析、索引与检索实现 |
| `oryxos-persona` | 人格预设与自定义模板库，采用 copy-in 模板复制语义 |
| `oryxos-cli` | Picocli 主入口、子命令与配置加载 |
| `oryxos-boot` | Spring Boot 启动、装配与依赖聚合 |

共享契约放 core，适配和持久化实现放对应边缘模块，通过接口进行依赖倒置。不要让 core 反向依赖 web、storage 或特定 IM 实现，不建立循环模块依赖。Tool 相关保持在一个 `oryxos-tool` 中，不重新拆成 builtin/skill/mcp 多个模块。

## 4. 运行时必须遵守的契约

### 统一执行入口与 ReAct

```text
CLI / REST / AgentScheduler / 已接入的 IM
                    ↓
               AgentService
                    ↓
                ReActLoop
                    ↓
    PromptBuilder + ContextLoader + MemoryService
                    ↓
              ProviderService
                    ↓
               ToolExecutor
                    ↓
           OryxTool / MCP / 外部系统
                    ↓
          工具结果回填 Session，继续推理
```

- 自实现 `ReActLoop`。Spring AI 仅用于 Provider 抽象、协议转换与 `@Tool` Schema 生成；**必须禁用 Spring AI 自动 Tool 执行**，实际调度只由 `ReActLoop` 与 `ToolExecutor` 完成。
- `ProviderService` 维护 provider name 到 `ChatModel` 的显式映射，不依赖扫描同类型 Bean 猜测 Provider。
- 默认最大迭代 10 次、上下文保留最近 20 轮，允许 Profile 覆盖；一次响应中的多个工具调用在核心阶段顺序执行。
- LLM 响应与工具结果完整追加到 Session。为模型截断上下文时，不得直接删除持久化审计所需的历史。
- Prompt 包含 Agent 指令、Bootstrap、绑定 Skill 元数据、当前日期时间、记忆、对话历史与可用工具。会话历史只能注入一次，避免门面与 PromptBuilder 重复拼接。
- 若使用 `ProfileContext` 传递当前 Profile，在请求入口设置，在 `finally` 清理，不能跨请求泄漏；后台线程和未来异步任务不能假定自动继承 ThreadLocal。

### Agent、Profile 与 Skill

- Agent 定义位于 `.oryxos/agents/<name>/AGENT.md`；frontmatter 派生 Profile，正文由 `ContextLoader` 注入 system prompt。Agent 目录不是一个可执行 Tool。
- 公共 Skill 实体位于 `.oryxos/skills/<name>/`；Agent 的 `skills/<name>` 使用指向公共实体的受控相对软链接，链接集合是绑定的唯一真相源，不另建 frontmatter `skills:` 绑定列表。
- 每轮重新读取 Agent 正文、Bootstrap 与绑定 Skill 元数据，不缓存。Skill 只预注入 name、description 和 Agent 本地读取路径，正文、参考与脚本按需经工具取用。
- 校验软链接的真实目标位于公共 Skill 根目录；未绑定 Skill 不应对 Agent 可见，删除被引用的 Skill 默认拒绝并返回引用 Agent。
- Agent 创建、启动扫描与热加载在相关能力实现时复用同一套派生、注册、调度逻辑，避免多个相互矛盾的注册入口。

### Memory

- `MemoryService` 是统一门面，内部协调会话与长期记忆；不要合并成仅有 Session 的存储服务。
- `LongTermMemoryStore` 隔离具体后端；技术方案定义 Markdown（默认）、SQLite、自托管 Mem0 三档，通过 `memory.backend` 选择。
- 核心区 `CORE` 全量保留，归档区 `ARCHIVAL` 可以截断；默认写归档，分区由 Agent 通过 `scope` 显式选择。
- 核心区不参与归档检索和向量索引；关键词检索不区分大小写。无明确失效机制前不增加缓存，写入后下一轮可见。
- 记忆本体先写成功，索引异常不阻断写入。混合检索和降级按技术方案第 5.1 节执行，不能为默认运行强制引入外部向量服务。
- Markdown 与默认 SQLite 都是单机落地选项；不能仅凭后端可替换或文档中的“共享 DB”表述，宣称已经支持安全的多副本共享和分布式协调。
- `USER.md` 是用户维护的只读 Bootstrap，`MEMORY.md` 是 Agent 经工具维护的长期记忆，不混用生命周期。

### Tool、Sandbox 与通知

- 所有内置工具、Java `@Tool` 与 MCP 工具统一适配为 `OryxTool`，经 `ToolRegistry` 注册和 `ToolExecutor` 执行；Profile 限定可用工具集合。
- 内置九个工具为 `read_file`、`write_file`、`list_dir`、`shell`、`http_get`、`http_post`、`save_memory`、`recall_memory`、`notify`。
- 使用 `Sandbox` 接口和 `WhitelistSandbox` 实现；不使用 Java `SecurityManager`，也不绕过路径、命令、网络白名单直接执行 IO。
- 文件边界校验要处理路径穿越与软链接真实路径；Shell 使用可执行文件和参数数组直传，执行超时受控，不拼接 Shell 命令字符串。
- 应用层白名单不是容器隔离。允许解释器等于授予宿主进程用户的代码执行权限，不能声称其提供文件/网络强隔离或多租户安全。
- MCP Server 与 Java 插件具有各自的信任边界，不能因为注册成 Tool 就认为其内部 IO 已被宿主白名单完整约束。
- `notify` 按名称从全局通知渠道注册表解析配置；通知渠道不写入 Profile 的 `notify_channels`，具体 webhook 和凭证不注入对话。
- 入站 Channel 与出站 Notify 是不同职责，分别适配；HTTP 通知发送同样经过网络白名单。

### 持久化、调度与 Web

- `sessions`、`tool_invocations`、`llm_calls` 是核心数据；工具与模型调用在核心阶段就写审计，不以日志替代。成功、失败和沙箱拒绝均有对应记录，模型调用记录 Provider、模型与可得的 token 使用量。
- 数据库演进采用 Flyway 的 vendor 分轨迁移；不要依赖 Hibernate 自动更新已有数据库，也不要新增平行的手工升级机制。
- `AgentScheduler` 使用动态 cron 注册，与人工触发共用 `AgentService`。按任务身份防止同一任务重叠，单实例锁不宣称为分布式锁。
- `schedules` 的定义来自 Agent 文件，数据库存状态和执行历史。全局 `schedule_id` 与 Agent 内 `schedule_key` 分离；不能用重复 key 静默选择首个任务。
- Controller 负责校验、响应包装与错误映射，业务行为委托服务层；沿用 `ApiResponse`（`code`、`message`、`data`、`timestamp`）信封。
- 核心 REST 门面包含 Session 创建/发消息/查询/归档、Agent invoke、Profile/Memory/Tool 查询、health/info 共 10 个端点，统一前缀 `/api/v1`；扩展端点按任务增补。
- 文档中的默认端口 8080、消息上限 32 KB、历史查询最近 100 条、Agent 请求 60 秒超时是待实现契约，不是已验证指标。
- API Key、JWT、RBAC、SSO、完整审计查询和多租户属于后续治理范围；当前无认证的内网运行假设不等于已具备生产级安全。

## 5. 实施与验证流程

1. 检查目录、构建文件、已有规范和当前改动，确定本次任务的范围与可交付结果。
2. 阅读对应章节；涉及旧表述时按第 2 节处理。先让一条真实链路可验证，再扩展到其他入口或适配器。
3. 主体开发沿用 `Provider → ReAct →（Memory / Tool）→ Web Service` 的能力依赖。大任务使用已有 Spec-Kit 流程；小修复和文档调整不强制重跑整套流程。
4. 使用 Spec-Kit 时维护 constitution、spec、plan、tasks 的对应关系，每个 User Story 完成后做一致性检查；工具可用时按指南执行 `/speckit.analyze`，不可用时明确说明并人工核对，不伪造执行结果。
5. 对行为变更验证成功路径与关键失败边界，尤其是工具仅执行一次、迭代退出、历史协议完整性、跨会话记忆、越界拒绝与失败审计。测试应验证行为，不机械复制实现。
6. 文档修改只做必要的结构、链接与内容核对；不为纯排版添加无意义代码测试。
7. 汇报改动、实际运行的验证及未验证部分，不把“代码已写”描述为“Demo 已跑通”。

### 构建命令

仅在 Maven 工程实际存在后执行；若存在 Maven Wrapper，优先使用仓库自带 Wrapper。

```bash
mvn clean package
mvn test
mvn -pl oryxos-core -am test
```

按变更选择相关模块或根构建，不在检查已通过后无理由重复测试。JAR 名称和启动参数从实际构建产物与 CLI 配置读取，不编造版本号、包名或尚未存在的可执行文件。

### 场景验收

按任务映射技术方案第 12 章的三个最新场景：每日天气、带公共 Skill 绑定和记忆偏好的科技日报、经专用 Tool/MCP 执行脚本的 GitHub 日报。共同验证定时触发、工具调用、通知、Session 历史和审计；同时保留 CLI 与 REST 的手动调用验证。不要把 GitHub 日报中的受控脚本工具替换成默认放开的通用解释器。

## 6. 协作、配置与文档维护

- 默认用中文沟通和撰写项目说明，代码标识符、API 与协议名保持一致。
- 变更聚焦本次请求，保护用户已有修改，不顺手生成业务代码、官网、部署脚本或历史提示词要求的其他产物。
- 凭证通过环境变量或受控配置加载，不硬编码、不写入示例真实值、不输出到日志和审计；缺失配置给出明确错误。
- Markdown 使用单一 H1、连续标题层级、代码块语言、清晰表格和必要的目录；文件路径与链接指向真实目标。
- 并行工作时明确文件或模块所有权，避免多个 Agent 同时修改同一文件；共享契约调整先协调，再汇总检查。
- 完成工作后更新受影响的说明和验收依据，不自行修改已确认的架构原则。不要以修改 spec、跳过测试或伪造外部调用掩盖失败。
- 本文件是共享开发规范的维护入口；`CLAUDE.md` 引用本文件，避免维护两套逐渐分叉的规则。
