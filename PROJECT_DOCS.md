# 📱 Market Android — Documentação Completa do Projeto

## 🧠 Conceito
Mercadinho autônomo 24h dentro de condomínios residenciais.
Um tablet fixo no estabelecimento onde o cliente escaneia o código de barras do produto, adiciona ao carrinho e paga via Pix direto na tela.

---

## 🔗 Links Importantes

| Serviço | URL | Credencial |
|---------|-----|-----------|
| **Backend Produção** | https://backend-production-f38c2.up.railway.app/api | — |
| **Railway** (Deploy) | https://railway.app | Login com GitHub |
| **GitHub** | https://github.com/uriartegui/market-android | — |
| **Neon** (PostgreSQL) | https://neon.tech | Login com conta |
| **Upstash** (Redis) | https://upstash.com | Login com conta |
| **Mercado Pago Dev** | https://www.mercadopago.com.br/developers | Login com conta |
| **Postman** | https://www.postman.com | guiuriarte@gmail.com |

---

## 🔐 Credenciais (Desenvolvimento)

```env
DATABASE_URL="postgresql://neondb_owner:npg_dlMof73pLXKV@ep-flat-moon-ac10gh8a-pooler.sa-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require"
REDIS_URL="rediss://default:gQAAAAAAAUFkAAIncDFkZmEyYWRiYTA2NjU0ZjIzYTk0Y2NlZjNkZjZlYTBmOHAxODIyNzY@select-shrimp-82276.upstash.io:6379"
JWT_SECRET="market-android-secret-2024-troque-em-producao"
JWT_EXPIRES_IN="1d"
JWT_REFRESH_SECRET="market-android-refresh-secret-2024-troque-em-producao"
JWT_REFRESH_EXPIRES_IN="7d"
MERCADOPAGO_ACCESS_TOKEN="TEST-8617996706084357-032315-aaad3511a071e9b045d9b43f77fe68f1-190560457"
PORT=3000
NODE_ENV=development
```

> ⚠️ NUNCA suba esse arquivo para o GitHub. Já está no .gitignore.

---

## 💻 Setup Local (PC Novo)

```cmd
git clone https://github.com/uriartegui/market-android.git
cd market-android\apps\backend
npm install
```

Crie o arquivo `apps/backend/.env` com as credenciais acima, depois:

```cmd
npx prisma generate
npm run start:dev
```

Servidor sobe em: http://localhost:3000/api

---

## 🏗️ Arquitetura

```
market-android/
├── apps/
│   └── backend/                 ← NestJS API
│       ├── prisma/
│       │   ├── schema.prisma    ← modelos do banco
│       │   └── migrations/      ← histórico de migrations
│       ├── src/
│       │   ├── auth/            ← login, registro, JWT
│       │   ├── users/           ← perfil do usuário
│       │   ├── condominio/      ← gestão de condomínios
│       │   ├── products/        ← produtos + QR Code + barcode
│       │   ├── orders/          ← pedidos + kiosk
│       │   ├── payments/        ← Pix via Mercado Pago
│       │   ├── prisma/          ← PrismaService
│       │   └── common/          ← guards, decorators, filters
│       ├── .env                 ← variáveis de ambiente (não sobe pro git)
│       ├── nixpacks.toml        ← config de build no Railway
│       └── package.json
├── packages/
│   └── shared/                  ← tipos compartilhados (futuro)
├── .env.example                 ← template do .env
├── .gitignore
└── docker-compose.yml           ← PostgreSQL + Redis local (se tiver Docker)
```

---

## 📡 Stack

| Camada | Tecnologia |
|--------|-----------|
| Backend | NestJS (Node.js + TypeScript) |
| ORM | Prisma v5 |
| Banco | PostgreSQL (Neon) |
| Cache/Fila | Redis (Upstash) |
| Auth | JWT + Refresh Token + bcrypt |
| Pagamento | Mercado Pago (Pix) |
| Deploy | Railway |
| CI/CD | Auto-deploy no push para main |

---

## 🗄️ Modelos do Banco

### User
```
id, name, email, password, phone, role, fcmToken
condominioId (FK), createdAt, updatedAt
```

### Condominio
```
id, name, address, code (único), createdAt, updatedAt
```

### Product
```
id, name, description, price, quantity, imageUrl
category, active, qrCode, barcode, condominioId (FK)
createdAt, updatedAt
```

### Order
```
id, status (PENDENTE/PAGO/ENTREGUE/CANCELADO)
total, userId? (FK - opcional para kiosk)
condominioId (FK), createdAt, updatedAt
```

### OrderItem
```
id, quantity, price, orderId (FK), productId (FK)
```

### Payment
```
id, orderId (FK único), status (PENDING/APPROVED/REJECTED/EXPIRED)
amount, pixQrCode, pixQrCodeBase64, mercadoPagoId
expiresAt, createdAt, updatedAt
```

### RefreshToken
```
id, token, userId (FK), expiresAt, createdAt
```

### Enums
```
Role: ADMIN, LOJISTA, MORADOR, KIOSK
OrderStatus: PENDENTE, PAGO, ENTREGUE, CANCELADO
PaymentStatus: PENDING, APPROVED, REJECTED, EXPIRED
```

---

## 📡 Endpoints da API

Base URL produção: `https://backend-production-f38c2.up.railway.app/api`
Base URL local: `http://localhost:3000/api`

### 🔐 Auth
| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| POST | /auth/register | ❌ | Criar conta |
| POST | /auth/login | ❌ | Login |
| POST | /auth/refresh | ❌ | Renovar token |
| POST | /auth/logout | ❌ | Logout |

### 👤 Usuários
| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| GET | /users/me | ✅ | Meu perfil |
| PUT | /users/me | ✅ | Atualizar perfil |

### 🏢 Condomínio
| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| POST | /condominios | ✅ | Criar condomínio |
| GET | /condominios | ✅ | Listar todos |
| GET | /condominios/:id | ✅ | Buscar por ID |
| POST | /condominios/join | ✅ | Entrar via código |

### 🛒 Produtos
| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| POST | /products | ✅ | Criar produto |
| GET | /products | ✅ | Listar produtos |
| GET | /products?category=X | ✅ | Filtrar por categoria |
| GET | /products/scan/:qrCode | ✅ | Buscar por QR Code |
| GET | /products/barcode/:barcode | ✅ | Buscar por código de barras |
| GET | /products/:id | ✅ | Buscar por ID |
| PUT | /products/:id | ✅ | Atualizar produto |
| DELETE | /products/:id | ✅ | Desativar produto |

### 📦 Pedidos
| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| POST | /orders | ✅ | Criar pedido (usuário) |
| POST | /orders/kiosk | ✅ | Criar pedido (tablet) |
| GET | /orders | ✅ | Listar meus pedidos |
| GET | /orders/:id | ✅ | Buscar pedido |
| PATCH | /orders/:id/status | ✅ | Atualizar status |

### 💳 Pagamentos
| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| POST | /payments/pix/:orderId | ✅ | Gerar QR Code Pix |
| GET | /payments/status/:orderId | ✅ | Status do pagamento |
| POST | /payments/webhook | ❌ | Webhook Mercado Pago |

---

## 🔄 Fluxo do Kiosk (Tablet)

```
1. Tablet logado com conta KIOSK do condomínio
2. Cliente escaneia barcode do produto
   → GET /api/products/barcode/:code
3. Produto adicionado ao carrinho (estado local no app)
4. Cliente adiciona mais produtos (repete passo 2)
5. Cliente confirma compra
   → POST /api/orders/kiosk { items: [...] }
6. Sistema gera Pix
   → POST /api/payments/pix/:orderId
7. QR Code Pix exibido na tela do tablet
8. Cliente paga com celular
9. App faz polling do status a cada 3s
   → GET /api/payments/status/:orderId
10. Pagamento aprovado → tela de sucesso
11. Estoque atualizado automaticamente
```

---

## 🚀 Próximos Passos

### Fase 2 — App Android (Tablet Kiosk)
```
[ ] Setup Android Studio (Kotlin + Jetpack Compose)
[ ] Tela de login do tablet (conta KIOSK)
[ ] Integração com câmera para scanner barcode (ML Kit)
[ ] Tela principal com carrinho
[ ] Tela de pagamento com QR Code Pix
[ ] Polling de status do pagamento (a cada 3s)
[ ] Tela de sucesso/erro
[ ] Modo quiosque (tela cheia, sem botão voltar)
```

### Fase 3 — Painel Admin Web
```
[ ] Dashboard de vendas
[ ] Cadastro de produtos com barcode
[ ] Gestão de estoque
[ ] Histórico de pedidos
[ ] Relatórios financeiros
[ ] Gestão de condomínios
```

### Fase 4 — App do Cliente
```
[ ] App Android do morador
[ ] Histórico de compras
[ ] Notificações push (FCM)
```

### Fase 5 — Produção
```
[ ] Trocar JWT_SECRET por string aleatória forte
[ ] Trocar Mercado Pago TEST → PRODUÇÃO
[ ] Configurar webhook URL de produção no Mercado Pago
[ ] Configurar domínio próprio
[ ] Monitoramento com Sentry
[ ] Backup automático do banco
```

---

## ⚠️ Segurança — Obrigatório antes de lançar

| Item | Status |
|------|--------|
| JWT_SECRET forte e aleatório | 🔴 Pendente |
| Mercado Pago em produção | 🔴 Pendente |
| Webhook URL de produção configurado | 🔴 Pendente |
| Resetar senha do Neon | 🟡 Recomendado |
| HTTPS ativo (Railway já faz) | ✅ Ok |
| Rate limiting ativo | ✅ Ok |
| Senhas com bcrypt | ✅ Ok |
| Inputs validados | ✅ Ok |

---

## 🛠️ Comandos Úteis

```cmd
# Subir servidor local
cd apps\backend
npm run start:dev

# Parar processo na porta 3000 (Windows)
netstat -ano | findstr :3000
taskkill /PID <numero> /F

# Matar todos os processos Node
taskkill /F /IM node.exe

# Prisma
npx prisma generate           # gerar client
npx prisma db push            # sincronizar schema com banco
npx prisma migrate dev --name <nome>  # criar migration
npx prisma studio             # abrir painel visual do banco

# Git
git add .
git commit -m "mensagem"
git push                      # Railway faz deploy automático
```

---

## 📱 Dados de Teste

```
Usuário:     guilherme@teste.com / 123456
Condomínio:  Residencial das Flores (código: FLORES01)
Produto:     Coca-Cola 2L (id: 1d3e853a-8e08-456d-a372-a7a7d9453dea)
```

---

## 📦 Dependências Principais

```json
{
  "@nestjs/common": "NestJS core",
  "@nestjs/jwt": "Autenticação JWT",
  "@nestjs/passport": "Estratégias de auth",
  "@nestjs/throttler": "Rate limiting",
  "@prisma/client": "ORM (v5)",
  "bcrypt": "Hash de senhas",
  "mercadopago": "SDK Mercado Pago",
  "qrcode": "Geração de QR Code",
  "class-validator": "Validação de DTOs",
  "class-transformer": "Transformação de dados"
}
```
