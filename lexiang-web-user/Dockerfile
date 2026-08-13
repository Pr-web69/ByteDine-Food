# ===== 阶段1：Vite 构建 =====
FROM node:20-alpine AS builder
WORKDIR /build

# 复制 package 文件（利用缓存层）
COPY package*.json ./
RUN npm install

# 复制源码
COPY . .

# 构建用户端 → dist/user
RUN VITE_APP_TYPE=user npm run build
# 构建商家端 → dist/admin
RUN VITE_APP_TYPE=admin npm run build

# ===== 阶段2：Nginx 运行 =====
FROM nginx:1.25-alpine
COPY --from=builder /build/dist/user /usr/share/nginx/user
COPY --from=builder /build/dist/admin /usr/share/nginx/admin
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
