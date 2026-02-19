# MTG Card Server 服务端测试报告

> **测试时间**: 2026-02-14
> **测试环境**: WSL2 Linux
> **服务器地址**: http://182.92.109.160:3000

---

## 📊 连接测试结果

### ✅ Ping 测试 - 成功

```
PING 182.92.109.160 (182.92.109.160): 56 data bytes
64 bytes from 182.92.109.160: icmp_seq=1 ttl=54 time=49.4 ms
64 bytes from 182.92.109.160: icmp_seq=2 ttl=54 time=10.1 ms

--- 182.92.109.160 ping statistics ---
2 packets transmitted, 2 received, 0% packet loss
rtt min/avg/max/mdev = 10.085/29.759/49.433/19.674 ms
```

**结论**: ✅ 服务器在线，网络延迟可接受（10-50ms）

---

### ❌ 端口连接测试 - 失败

```
nc -zv 182.92.109.160 3000
nc: connect to 182.92.109.160 port 3000 (tcp) timed out: Operation now in progress
```

**结论**: ❌ TCP 端口 3000 无法连接

---

### ⚠️ HTTP API 测试 - 超时

**测试命令**:
```bash
curl -s "http://182.92.109.160:3000/api/result?q=闪电箭&page=1&page_size=5"
```

**结果**: ❌ 请求超时（> 5 秒）

---

## 🔍 问题诊断

### 可能的原因

1. **🔴 服务未启动**
   - MTG Card Server 进程可能没有运行
   - Node.js/Express 服务可能已停止

2. **🔴 防火墙阻止**
   - 阿里云安全组可能未开放 3000 端口
   - 服务器本地防火墙（iptables/firewalld）可能阻止入站连接

3. **🟡 服务配置错误**
   - 服务可能监听在 `localhost` 而非 `0.0.0.0`
   - 端口配置可能不是 3000

4. **🟡 Nginx/反向代理未配置**
   - 可能需要通过 Nginx 代理访问
   - 实际端口可能不同（如 80, 8080）

---

## ✅ 服务端检查清单

### 在服务器上执行以下命令检查：

```bash
# 1. 检查 Node.js 进程是否运行
ps aux | grep node
# 或
pm2 list

# 2. 检查端口 3000 是否被监听
netstat -tuln | grep 3000
# 或
ss -tuln | grep 3000

# 3. 检查防火墙状态
sudo iptables -L -n
# 或（CentOS/RHEL）
sudo firewall-cmd --list-ports

# 4. 检查服务日志
pm2 logs mtg-card-server --lines 50
# 或
tail -f /path/to/mtg-card-server/logs/*.log

# 5. 测试本地访问
curl "http://localhost:3000/api/result?q=测试&page=1&page_size=5"
```

---

## 🔧 解决方案

### 方案 1: 启动服务（如果未运行）

```bash
# 进入项目目录
cd /path/to/mtg-card-server

# 安装依赖（如果需要）
npm install

# 启动服务（开发模式）
npm run dev

# 或使用 PM2（生产模式）
pm2 start npm --name "mtg-card-server" -- start

# 查看日志
pm2 logs mtg-card-server
```

### 方案 2: 配置防火墙

**阿里云安全组**:
1. 登录阿里云控制台
2. 进入 ECS 实例 → 安全组
3. 添加入站规则：
   - 端口范围: 3000/3000
   - 授权对象: 0.0.0.0/0（或特定 IP）
   - 协议: TCP

**服务器本地防火墙**:
```bash
# CentOS/RHEL (firewalld)
sudo firewall-cmd --permanent --add-port=3000/tcp
sudo firewall-cmd --reload

# Ubuntu/Debian (ufw)
sudo ufw allow 3000/tcp
sudo ufw reload

# 临时关闭防火墙测试（不推荐）
sudo systemctl stop firewalld  # CentOS/RHEL
# 或
sudo ufw disable  # Ubuntu
```

### 方案 3: 修改服务监听地址

**检查服务配置文件**（如 `app.js`, `index.js`, 或 `.env`）:

```javascript
// ❌ 错误：仅监听本地
app.listen(3000, 'localhost', () => {
  console.log('Server running on localhost:3000')
})

// ✅ 正确：监听所有接口
app.listen(3000, '0.0.0.0', () => {
  console.log('Server running on 0.0.0.0:3000')
})

// 或不指定地址（默认所有接口）
app.listen(3000, () => {
  console.log('Server running on port 3000')
})
```

### 方案 4: 配置 Nginx 反向代理（可选）

**如果使用 Nginx，配置**:

```nginx
# /etc/nginx/sites-available/mtg-card-server
server {
    listen 80;
    server_name your-domain.com;  # 或服务器 IP

    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}

# 启用配置
sudo ln -s /etc/nginx/sites-available/mtg-card-server /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重载 Nginx
sudo systemctl reload nginx
```

**然后访问**: `http://your-domain.com/api/result?q=测试`

---

## 🧪 服务端启动后测试

### 测试 1: 基础健康检查

```bash
curl -w "\nHTTP Status: %{http_code}\nTime: %{time_total}s\n" \
  "http://182.92.109.160:3000/api/result?q=闪电箭&page=1&page_size=5"
```

**预期响应**:
```json
{
  "success": true,
  "count": 1,
  "page": 1,
  "page_size": 5,
  "total_pages": 1,
  "items": [
    {
      "id": 12345,
      "oracle_id": "abc123",
      "scryfall_id": "xyz789",
      "name": "Lightning Bolt",
      "zh_name": "闪电箭",
      "mana_cost": "{R}",
      "cmc": 1.0,
      "type_line": "Instant",
      "oracle_text": "Deal 3 damage to any target.",
      "colors": ["R"],
      "rarity": "common",
      "set_code": "LEA",
      "set_name": "Limited Edition Alpha"
    }
  ]
}
```

### 测试 2: 随机卡牌

```bash
curl "http://182.92.109.160:3000/api/random"
```

### 测试 3: 获取所有系列

```bash
curl "http://182.92.109.160:3000/api/sets"
```

### 测试 4: 单卡详情

```bash
curl "http://182.92.109.160:3000/api/cards/12345"
```

---

## 📝 Android 端测试建议

### 在服务端可用后：

1. **编译并安装应用**
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

2. **查看应用日志**
   ```bash
   adb logcat | grep -E "DecklistManager|SearchViewModel|DecklistRepository"
   ```

3. **测试搜索功能**
   - 打开应用
   - 进入搜索页面
   - 搜索 "闪电箭" 或 "Lightning Bolt"
   - 查看日志输出和网络请求

4. **预期日志**
   ```
   D/SearchViewModel: Searching: 闪电箭
   D/DecklistRepository: Cache miss for: 闪电箭, fetching from API
   D/SearchViewModel: Found 1 results
   ```

---

## 📞 联系服务端开发团队

**需要提供给服务端团队的信息**:

1. ✅ 服务器 IP 可访问（ping 通）
2. ❌ TCP 端口 3000 无法连接
3. ❌ HTTP API 请求超时

**建议询问**:
- MTG Card Server 服务是否正在运行？
- 防火墙是否已开放 3000 端口？
- 服务是否监听在 `0.0.0.0:3000` 而非 `localhost:3000`？
- 是否需要配置 Nginx 反向代理？
- 服务端日志中是否有错误信息？

---

## 🔄 后续行动

### 立即行动（服务端团队）
1. 检查服务运行状态
2. 检查防火墙配置
3. 验证服务监听地址
4. 提供服务端日志

### 待服务端可用后（Android 端）
1. 完成剩余的 API 迁移工作
2. 更新 `DecklistRepository.fetchScryfallDetails()`
3. 编译并测试搜索功能
4. 验证图片加载（Scryfall API）
5. 测试缓存功能

### 当前状态
- ✅ Android 端代码已准备好（约 70% 完成）
- ⏳ 等待服务端可用
- ⏳ 需要集成测试

---

**最后更新**: 2026-02-14
**测试人**: Claude Code Assistant
**下一步**: 联系服务端团队检查服务状态
