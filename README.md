# 企业内部 IAM 平台

面向企业内部员工的身份与访问管理（IAM）。同一进程提供身份 API、OIDC / OAuth 2.0 授权服务器，以及简体中文的登录、门户与管理控制台前端。

当前交付覆盖：数据模型、Flyway 建表、用户 / 组织 / 角色 / 口令、CSV 人员导入解析、应用目录与按人/组织/角色授权、ABAC 策略管理与决策点（PDP）、会话与只读审计 API，基于 Spring Authorization Server 的 OIDC（授权码 + PKCE、客户端凭证、发现、UserInfo、刷新令牌轮换、授权许可页），以及 Vite + React 单页应用（登录、强制改密、SSO 门户、自助资料与会话、管理控制台）。

- 运行时：Java 21、Spring Boot **4.1.2**、Spring Data JPA、PostgreSQL 16、Flyway、Spring Security 7.1、Spring Authorization Server（starter：`spring-boot-starter-security-oauth2-authorization-server`）、Actuator
- 包名：`com.enterprise.iam`
- IAM 是身份主数据来源；用户只做状态变更，不做物理删除（`terminated`）
- 本仓库的编码约定（含测试先行 / TDD）见 [AGENTS.md](./AGENTS.md)

## 前置条件

- JDK 21（构建与运行都需要）
- 本地运行需要 PostgreSQL 16；可用下面的 Docker Compose 启动
- 项目已包含 Maven Wrapper（`./mvnw`），安装 JDK 后不必再单独安装 Maven
- 构建前端需要 Node.js。根目录执行 `./mvnw` 时，`iam-frontend` 模块会通过 frontend-maven-plugin 下载固定的 Node LTS 并完成构建，无需预先安装 Node。仅做前端开发时，也可在 `iam-frontend/` 下使用本机 `npm`。
- 本机若无 Docker，集成测试仍可运行：测试使用嵌入式 PostgreSQL 16（Zonky），不依赖 Docker 或 H2

## 启动数据库

```bash
docker compose up -d
```

默认连接（仅开发环境，勿用于生产）：

- JDBC：`jdbc:postgresql://localhost:5432/iam`
- 用户 / 密码：`iam` / `iam_dev_only`
- 库名：`iam`

## 启动应用

```bash
export JAVA_HOME=/path/to/jdk-21
# 开发环境启用引导管理员与演示 OIDC 客户端
# -am 会先构建 iam-frontend web jar，再启动后端
./mvnw -pl iam-backend -am spring-boot:run -Dspring-boot.run.profiles=dev
```

或先打包再运行可执行 Spring Boot jar：

```bash
./mvnw -DskipTests package
java -Dspring.profiles.active=dev -jar iam-backend/target/iam-backend-0.1.0-SNAPSHOT.jar
```

健康检查：`GET http://localhost:8080/actuator/health`（无需登录）。

浏览器打开 `http://localhost:8080/`，未登录会跳转到 `/login`。前端以 web jar（`com.enterprise:iam-frontend`）依赖的方式打进后端，静态资源位于 `META-INF/resources/`（`index.html` 与 `/assets/**` 从应用根路径提供，而不是 `/webjars/...`），因此上述命令同时提供 UI 与 API。

### 仅前端开发

后端先以 `dev` 配置在 8080 端口运行，再启动 Vite 开发服务器（将 `/api`、`/login`（仅 POST）、`/logout`、`/oauth2`、`/userinfo`、`/csrf` 等代理到 8080）：

```bash
cd iam-frontend
npm install
npm run dev
```

浏览器访问 Vite 地址（默认 `http://localhost:5173`）。Vite 仍将 `/api`、`/login`（仅 POST）、`/logout`、`/oauth2`、`/userinfo`、`/csrf` 等代理到 8080。前端单元测试：在 `iam-frontend/` 下执行 `npm test`，或通过根目录 `./mvnw test` 在前端模块的 `test` 阶段运行。

### 前端界面

界面语言为简体中文。主要页面：

| 路径 | 说明 |
| --- | --- |
| `/login` | 表单登录，提交到 Spring Security `/login`，供门户与 `/oauth2/authorize` 共用 |
| `/`、`/portal` | 登录后的 SSO 门户，展示可启动的应用卡片 |
| `/password` | 修改口令；若 `mustChangePassword` 为真，会挡住其他页面 |
| `/profile`、`/sessions` | 个人资料与会话自助（可注销当前/其他会话） |
| `/admin/**` | 仅 `ROLE_ADMIN`：用户、组织、角色、应用目录与授权、访问策略、CSV 导入、只读审计、会话管理 |
| `/consent` | OIDC 授权许可页（演示客户端 `iam-demo-consent` 会进入此页） |

生产构建在 `iam-frontend` 模块中完成：Vite 把产物写入该模块 jar 的 `META-INF/resources/`，后端通过 Maven 依赖引入这个 web jar。`./mvnw test` 会按反应器顺序同时运行前端 Vitest 与后端集成测试。

### 浏览器会话与 CSRF

浏览器请使用表单登录后的会话 Cookie，**不要在浏览器里使用 HTTP Basic**。API 客户端仍可使用 HTTP Basic 或 Bearer JWT。

`/login` 与 `/logout`（以及授权许可表单 POST `/oauth2/authorize`）受 CSRF 保护。Cookie 名称为 `XSRF-TOKEN`（非 HttpOnly）。本服务 CSRF 令牌的请求头名为 `X-CSRF-TOKEN`（`GET /csrf` 的 `headerName` 为准），表单隐藏域为 `_csrf`。也可访问 `GET /csrf` 获取令牌并写入 Cookie。

为兼容现有的 Basic / Bearer API 客户端，`/api/v1/**` 与 `/actuator/**` **当前忽略 CSRF**。前端对变更类 API 仍会附带 CSRF 头，以便日后收紧策略。不要削弱登录 CSRF。


### 开发环境默认管理员

仅在 `dev`（或显式打开 `iam.bootstrap.admin.enabled=true` 的）配置下创建，且**仅在该用户尚不存在时**写入，不会在重启时重置口令。

| 项 | 值 |
| --- | --- |
| 用户名 | `admin` |
| 默认口令 | `ChangeMe_Admin_Dev_Only1!` |

可用环境变量覆盖口令：

```bash
export IAM_BOOTSTRAP_ADMIN_PASSWORD='your-strong-dev-password'
```

生产环境不要启用引导账号，也不要把该口令写入配置仓库。管理员角色编码为 `admin`（Spring Security 中为 `ROLE_ADMIN`）。

## OIDC / OAuth 2.0 授权服务器

同一进程内集成 Spring Authorization Server。Issuer 可通过 `iam.oidc.issuer` 配置，开发默认值为 `http://localhost:8080`。发现文档：

```bash
curl http://localhost:8080/.well-known/openid-configuration
```

协议端点（均由 issuer 派生）：

| 用途 | 路径 |
| --- | --- |
| 授权 | `/oauth2/authorize` |
| 令牌 | `/oauth2/token` |
| JWKS | `/oauth2/jwks` |
| 撤销 | `/oauth2/revoke` |
| 探视 | `/oauth2/introspect` |
| UserInfo | `/userinfo` |
| RP 发起的登出 | `/connect/logout` |

访问令牌默认 10 分钟（5–15 分钟区间），授权码 5 分钟，刷新令牌 8 小时。刷新令牌绑定会话与客户端，使用后轮换；重放已轮换的刷新令牌会返回 400 并吊销该令牌族。登录成功会创建 `iam_session`；会话结束或登出时会吊销相关刷新令牌。ID Token 与 UserInfo 的 `sub` 为 `iam_user.id`（UUID）；在请求了相应 scope 时返回 `profile` / `email` 标准声明。

`/api/v1/**` 仍以 HTTP Basic 为主（也可附带 Bearer JWT）。`/actuator/health` 与 `/actuator/info` 保持匿名可访问。

开发签名密钥：首次启动时在 `.data/dev-signing-jwk.json` 生成 RSA JWK（该目录已 gitignore）。生产环境应从密钥保险库加载，不要使用该文件，也不要把签名私钥写入业务库。

### 开发环境演示客户端

仅在 `dev` / `test`（或显式打开 `iam.bootstrap.clients.enabled=true`）且客户端尚不存在时写入，**不会在重启时重置已有密钥**。下列密钥仅用于开发，勿用于生产。

| 客户端 | 授权类型 | Scope | 密钥（可用环境变量覆盖） |
| --- | --- | --- | --- |
| `iam-demo-web` | `authorization_code` + `refresh_token`，要求 PKCE，不强制同意页 | `openid profile email` | `DevWeb_ClientSecret_Only!`（`IAM_DEMO_WEB_SECRET`） |
| `iam-demo-api` | `client_credentials` | `api` | `DevApi_ClientSecret_Only!`（`IAM_DEMO_API_SECRET`） |
| `iam-demo-consent` | 与 web 相同，但 `requireConsent=true`，用于演练同意页 | `openid profile email` | `DevConsent_ClientSecret_Only!`（`IAM_DEMO_CONSENT_SECRET`） |

`iam-demo-web` 的回调地址：

- `http://localhost:8080/login/oauth2/code/iam-demo-web`
- `http://127.0.0.1:8080/login/oauth2/code/iam-demo-web`

`iam-demo-consent` 的回调地址为对应的 `/login/oauth2/code/iam-demo-consent`。登录后会先进入 `/consent` 许可页。

客户端密钥仅以 Argon2id 哈希（`iam_oauth_client.client_secret_hash`）落库。

### 客户端凭证（机器对机器）

```bash
curl -u 'iam-demo-api:DevApi_ClientSecret_Only!' \
  -d 'grant_type=client_credentials' \
  -d 'scope=api' \
  http://localhost:8080/oauth2/token
```

### 授权码 + PKCE（浏览器应用）

1. 生成 PKCE `code_verifier` / `code_challenge`（S256）：

```bash
python3 - << 'PY'
import base64, hashlib, secrets
verifier = base64.urlsafe_b64encode(secrets.token_bytes(32)).rstrip(b"=").decode()
challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode("ascii")).digest()).rstrip(b"=").decode()
print("VERIFIER=" + verifier)
print("CHALLENGE=" + challenge)
PY
```

2. 在浏览器打开（将 `CHALLENGE` 换成上一步的值）：

```
http://localhost:8080/oauth2/authorize?response_type=code&client_id=iam-demo-web&redirect_uri=http://localhost:8080/login/oauth2/code/iam-demo-web&scope=openid%20profile%20email&state=xyz&code_challenge=CHALLENGE&code_challenge_method=S256
```

3. 在表单登录页使用开发管理员账号登录（默认 `admin` / `ChangeMe_Admin_Dev_Only1!`）。
4. 浏览器会被重定向到回调地址并带上 `code`。本服务不托管该客户端回调页，页面可能显示 404，从地址栏复制 `code` 即可。
5. 用授权码换令牌，并访问 UserInfo：

```bash
curl -u 'iam-demo-web:DevWeb_ClientSecret_Only!' \
  -d 'grant_type=authorization_code' \
  -d 'code=PASTE_CODE' \
  -d 'redirect_uri=http://localhost:8080/login/oauth2/code/iam-demo-web' \
  -d 'code_verifier=PASTE_VERIFIER' \
  http://localhost:8080/oauth2/token

curl -H "Authorization: Bearer PASTE_ACCESS_TOKEN" \
  http://localhost:8080/userinfo
```

刷新令牌：

```bash
curl -u 'iam-demo-web:DevWeb_ClientSecret_Only!' \
  -d 'grant_type=refresh_token' \
  -d 'refresh_token=PASTE_REFRESH_TOKEN' \
  http://localhost:8080/oauth2/token
```

## 身份 API

基础路径：`/api/v1`，JSON。错误体为 RFC 7807 Problem Detail。除健康检查与 OIDC 协议端点外，API 需已认证（浏览器会话 Cookie、HTTP Basic 或 Bearer JWT）。写操作（创建 / 更新用户、组织、角色与应用、授权、策略发布、重置口令、导入任务、管理员注销会话）需要 `ADMIN` 角色。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/users` | 创建用户（可选口令、主组织、属性） |
| `GET` | `/api/v1/users/{id}` | 按 ID 查询（含已分配角色） |
| `GET` | `/api/v1/users?username=&status=` | 搜索 / 分页 |
| `PATCH` | `/api/v1/users/{id}` | 更新资料或状态（`DISABLED` / `TERMINATED` 等，不物理删除） |
| `POST` | `/api/v1/users/{id}/assignments` | 分配组织（可设为主组织） |
| `PUT` | `/api/v1/users/{id}/attributes` | 覆盖设置用户属性 |
| `POST` | `/api/v1/users/{id}/password` | 管理员设置 / 重置口令 |
| `GET` | `/api/v1/users/{id}/roles` | 列出用户已分配角色 |
| `POST` | `/api/v1/users/{id}/roles` | 为用户分配角色 |
| `DELETE` | `/api/v1/users/{id}/roles/{roleId}` | 移除用户角色（不能移走最后一名管理员的 admin 角色） |
| `GET` | `/api/v1/roles` | 角色列表（分页） |
| `POST` | `/api/v1/roles` | 创建角色（code 唯一） |
| `GET` | `/api/v1/roles/{id}` | 角色详情 |
| `PATCH` | `/api/v1/roles/{id}` | 更新角色名称、描述或状态 |
| `DELETE` | `/api/v1/roles/{id}` | 删除角色（内置 admin 角色不可删除） |
| `GET` | `/api/v1/me` | 当前用户资料、是否必须改密、是否管理员与 authorities |
| `POST` | `/api/v1/me/password` | 修改自己的口令 |
| `GET` | `/api/v1/me/applications` | 当前用户可启动的应用：无授权记录的应用对所有已登录用户可见；有授权时需匹配用户、所属组织或角色。若已有已发布策略作用于该应用的 `app:launch`，还需 PDP 允许 |
| `GET` | `/api/v1/me/sessions` | 当前用户的会话列表（含当前会话标记） |
| `DELETE` | `/api/v1/me/sessions/{id}` | 注销自己的一条会话 |
| `POST` | `/api/v1/me/sessions/revoke-others` | 注销除当前外的其他会话 |
| `GET` | `/api/v1/applications` | 管理端应用列表（可按 `q`、状态筛选） |
| `POST` | `/api/v1/applications` | 创建应用（含启动地址） |
| `GET` | `/api/v1/applications/{id}` | 应用详情 |
| `PATCH` | `/api/v1/applications/{id}` | 更新应用 |
| `GET` | `/api/v1/applications/{id}/entitlements` | 列出应用授权 |
| `POST` | `/api/v1/applications/{id}/entitlements` | 添加授权（subjectType：USER / ORG / ROLE） |
| `DELETE` | `/api/v1/applications/{id}/entitlements/{entitlementId}` | 移除授权 |
| `POST` | `/api/v1/policies` | 创建策略（草稿） |
| `GET` | `/api/v1/policies` | 策略列表 |
| `GET` | `/api/v1/policies/{id}` | 策略详情 |
| `POST` | `/api/v1/policies/{id}/versions` | 新增策略版本（JSON 文档，自动计算 checksum） |
| `GET` | `/api/v1/policies/{id}/versions` | 列出策略版本 |
| `POST` | `/api/v1/policies/{id}/versions/{versionId}/publish` | 发布策略版本 |
| `POST` | `/api/v1/policies/{id}/bindings` | 将已发布版本绑定到应用或组织 |
| `GET` | `/api/v1/policies/{id}/bindings` | 列出策略绑定 |
| `POST` | `/api/v1/pdp/decide` | 对当前用户做决策（默认拒绝；拒绝优先于允许；未发布版本不生效） |
| `GET` | `/api/v1/sessions` | 管理端按用户名/状态查询会话 |
| `POST` | `/api/v1/sessions/{id}/revoke` | 管理员注销会话 |
| `GET` | `/api/v1/audit-events` | 只读审计查询（时间、操作者、动作、对象） |
| `GET` | `/api/v1/audit-events/{id}` | 单条审计事件 |
| `POST` | `/api/v1/orgs` | 创建组织 |
| `GET` | `/api/v1/orgs` | 列出某父节点下的子组织（`parentId` 为空则为根） |
| `GET` | `/api/v1/orgs/tree` | 组织树 |
| `GET` | `/api/v1/orgs/{id}` | 组织详情 |
| `GET` | `/api/v1/orgs/{id}/children` | 直接子节点 |
| `PATCH` | `/api/v1/orgs/{id}` | 更新组织（名称、状态、排序、父节点） |
| `POST` | `/api/v1/import-jobs` | 创建导入任务。JSON 仅含 `fileRef` 时仍只登记为 pending；提供 `csv` 字段或上传 `multipart` CSV 文件时会解析并执行（按 username 创建或更新，不物理删除，不读取 file:// 本地文件） |
| `GET` | `/api/v1/import-jobs/{id}` | 查询导入任务 |
| `GET` | `/api/v1/import-jobs` | 分页列出导入任务 |
| `GET` | `/actuator/health` | 健康检查 |
| `GET` | `/actuator/info` | 应用信息 |

调用示例：

```bash
curl -u admin:ChangeMe_Admin_Dev_Only1! \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","displayName":"Alice","password":"Alice-Passw0rd!"}' \
  http://localhost:8080/api/v1/users
```

口令策略：长度 12–128，至少包含一个字母和一个数字；使用 **Argon2id** 哈希存储，接口响应中不会返回哈希。近期口令不可复用；连续失败达到阈值会锁定账户。

JSON 策略文档形状（内建 PDP，不嵌入 OPA 或 Cedar）：

```json
{
  "effect": "allow",
  "actions": ["app:launch"],
  "resources": [{ "type": "application", "id": "*", "code": "finance-app" }],
  "when": {
    "roles": ["finance"],
    "orgCodes": ["eng"],
    "attributes": { "title": "Engineer" }
  }
}
```

`when` 可省略。决策时拒绝优先于允许；没有匹配的已发布并已绑定策略时，`/api/v1/pdp/decide` 默认拒绝。决策始终针对当前登录用户。

CSV 导入列：`username,display_name,email,org_code,status,temporary_password`。按用户名创建或更新；提供临时口令时用户下次登录必须修改；`org_code` 不存在则该行失败。响应与错误报告中不会出现明文口令。


## 模块结构

本仓库是 Maven 多模块构建（父工程 `com.enterprise:iam-platform:0.1.0-SNAPSHOT`，packaging `pom`）：

```
iam-platform/                 # 父 POM
  iam-frontend/               # com.enterprise:iam-frontend，packaging jar（web jar，非 war，也不做 Spring Boot repackage）
  iam-backend/                # com.enterprise:iam-backend，Spring Boot 可执行 jar
```

反应器顺序为先 `iam-frontend`、后 `iam-backend`。后端依赖：

```xml
<dependency>
  <groupId>com.enterprise</groupId>
  <artifactId>iam-frontend</artifactId>
  <version>${project.version}</version>
</dependency>
```

可运行产物为 `iam-backend/target/iam-backend-0.1.0-SNAPSHOT.jar`。

Java 包仍位于 `iam-backend` 模块中：

```
com.enterprise.iam
  config        安全与引导配置
  identity      用户、组织、凭证、角色、导入任务
  auth          HTTP Basic / 表单登录 / UserDetails
  session       会话、授权码与刷新令牌持久化
  application   应用目录、启动地址、授权与 OAuth 客户端
  policy        ABAC 策略管理、绑定与 PDP 决策
  audit         只追加的审计服务与只读查询
  web           前端 SPA 回退与 CSRF 辅助接口
  pam           PAM 预留表与实体（无业务 API）
  oidc          Spring Authorization Server 配置与 JPA 适配
  common        异常与 Problem Detail
```

数据库迁移：`iam-backend/src/main/resources/db/migration/V1__init_schema.sql`、`V2__oidc_authorization.sql`、`V3__application_launch_url.sql`、`V4__application_entitlement.sql`。应用使用 `ddl-auto=validate`，表结构由 Flyway 管理。授权码、刷新令牌与客户端密钥只存哈希，不存明文。

## 构建与测试

```bash
./mvnw test
./mvnw package
```

在仓库根目录执行即可构建整个反应器。`./mvnw test` 会先构建并测试 `iam-frontend`（Vitest），再运行 `iam-backend` 的 Surefire 集成测试。测试会启动嵌入式 PostgreSQL 16（Zonky），执行 Flyway，并覆盖：用户创建与用户名唯一约束、口令不为明文且为 Argon2id、审计行写入、组织与改密、禁用后无法登录、CSV 导入创建/更新与失败行报告；角色 CRUD 与分配；应用目录、门户应用列表与按用户/组织/角色授权；ABAC 策略发布、绑定、默认拒绝与拒绝覆盖允许；会话自助与管理员注销、只读审计过滤、表单登录会话调用 API、登录错误分类与 CSRF；前端 web jar 中的 `META-INF/resources/index.html` 作为登录页与 SPA 回退；以及 OIDC 发现文档、授权码 + PKCE（`sub` 为用户 UUID、UserInfo）、需要许可的授权码流程、客户端凭证、刷新令牌轮换与重放拒绝、健康检查与 HTTP Basic 管理 API 仍可用。

只打包、跳过测试：

```bash
./mvnw -DskipTests package
```

## 本期范围 / 后续工作

**已包含**

- 项目骨架（Boot 4.1.2 starter、Wrapper、Compose、配置）
- 设计中的全部表与 JPA 实体（含 PAM 预留）
- 用户 / 组织 / 角色 / 口令 / CSV 人员导入解析 / 应用目录与授权 / 会话 / 只读审计 API
- ABAC 策略管理（创建、版本、发布、绑定）与 `/api/v1/pdp/decide` 决策点
- 简体中文门户与管理控制台前端（随 Spring Boot 一同提供）
- Argon2id、审计追加、HTTP Basic 与表单登录、开发环境管理员引导
- OIDC / OAuth 2.0 授权服务器：发现、授权码 + PKCE、客户端凭证、ID Token / UserInfo、撤销与探视、RP 登出、刷新令牌轮换与重放吊销、JPA 持久化与开发客户端引导

**明确不做（后续迭代）**

- 完整的 SSO 门户与正式同意（consent）界面打磨
- MFA 校验
- Redis 热会话
- PAM 申请 / 签出 / 录像等业务 API
- 更细粒度的操作审计检索 API
- 生产级密钥保险库与密钥轮换（签名密钥不得进入业务库，仅保存 `vault_ref` 或从保险库加载）
