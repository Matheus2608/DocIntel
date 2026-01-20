# Spec-Driven Development (SDD) com Tessl

## O que é SDD?

Spec-Driven Development é uma abordagem onde você escreve **especificações antes do código**. A spec se torna a fonte de verdade para humanos e agentes de IA, capturando intenções de forma clara e estruturada.

### Benefícios do SDD

- **Alinhamento**: Specs fornecem alinhamento entre o desenvolvedor e o agente de IA
- **Memória de longo prazo**: Specs armazenadas no codebase permitem que agentes evoluam a aplicação
- **Menos alucinações**: Specs explicam como usar bibliotecas externas, evitando erros de API
- **Documentação viva**: A spec é a documentação que se mantém atualizada

## Tessl no DocIntel

### O que foi configurado?

1. **Tessl CLI** (v0.57.3) - Instalado globalmente via npm
2. **MCP Server** - Integração com Claude Code (`.mcp.json`)
3. **Manifesto** - Arquivo `tessl.json` com metadados do projeto
4. **Spec Registry** - Acesso a mais de 10.000 specs de bibliotecas open source

### Estrutura de arquivos

```
DocIntel/
├── .mcp.json           # Configuração do MCP server para Claude Code
├── tessl.json          # Manifesto do projeto com dependências
└── .tessl/             # Diretório de configuração do tessl
    └── .gitignore
```

## Como usar o Tessl

### 1. Autenticação (necessário para funcionalidades completas)

```bash
tessl login
```

Este comando irá:
- Abrir o navegador para autenticação via WorkOS
- Armazenar suas credenciais localmente
- Habilitar acesso ao Spec Registry

### 2. Pesquisar specs no Registry

```bash
# Pesquisar specs para uma biblioteca
tessl search quarkus
tessl search react
tessl search postgresql

# Pesquisar por funcionalidade específica
tessl search "websocket authentication"
```

### 3. Instalar specs no projeto

```bash
# Instalar spec de uma biblioteca
tessl install quarkus/rest
tessl install react/hooks

# Instalar e sincronizar dependências automaticamente
tessl install --project-dependencies quarkus/hibernate
```

### 4. Listar specs instaladas

```bash
# Ver todas as specs instaladas no projeto
tessl list

# Formato JSON para processamento
tessl list --json
```

### 5. Criar suas próprias specs

As specs devem ser criadas como "tiles" (componentes reutilizáveis):

```bash
# Validar estrutura de uma spec local
tessl tile lint ./specs/minha-spec

# Empacotar uma spec para distribuição
tessl tile pack ./specs/minha-spec

# Publicar no registry (requer autenticação)
tessl tile publish ./specs/minha-spec
```

### 6. Verificar status

```bash
# Ver diagnóstico completo do tessl
tessl doctor

# Ver informações do usuário logado
tessl whoami
```

## Workflow SDD recomendado

### Para novas features

1. **Escreva a spec primeiro**
   - Descreva o que você quer construir
   - Defina interfaces, comportamentos esperados
   - Especifique dependências e requisitos

2. **Use o agente de IA com a spec**
   - O Claude Code lerá a spec via MCP
   - A IA terá contexto completo sobre bibliotecas usadas
   - Implementação seguirá fielmente a especificação

3. **Teste e refine**
   - Valide se a implementação atende a spec
   - Atualize a spec conforme aprende mais
   - Mantenha spec e código sincronizados

### Para usar bibliotecas externas

Ao invés de deixar a IA "adivinhar" como usar uma biblioteca:

1. **Pesquise no registry**: `tessl search nome-da-biblioteca`
2. **Instale a spec oficial**: `tessl install workspace/biblioteca`
3. **Implemente com confiança**: A IA saberá exatamente como usar a biblioteca

## Exemplo prático: Adicionar autenticação JWT

### Abordagem tradicional (sem SDD)
```
"Adicione autenticação JWT usando Quarkus"
→ IA pode usar APIs desatualizadas
→ Pode misturar versões incompatíveis
→ Documentação pode estar desatualizada
```

### Abordagem SDD (com Tessl)
```bash
# 1. Pesquisar spec oficial
tessl search "quarkus jwt"

# 2. Instalar spec
tessl install quarkus/jwt-auth

# 3. Criar spec customizada (opcional)
# specs/authentication.md
"""
## Autenticação JWT para DocIntel

### Requisitos
- Usar Quarkus JWT RBAC
- Tokens válidos por 24h
- Refresh tokens por 7 dias
- Roles: USER, ADMIN

### Endpoints
- POST /api/auth/login -> retorna access_token
- POST /api/auth/refresh -> renova token
- POST /api/auth/logout -> invalida token
"""

# 4. Pedir para a IA implementar baseado na spec
"Implemente a autenticação conforme specs/authentication.md usando a spec do Quarkus JWT"
```

## Tessl Products

### Tessl Spec Registry (Disponível - Open Beta)
- ✅ Mais de 10.000 specs de bibliotecas open source
- ✅ Gratuito para usar
- ✅ Previne alucinações de API
- ✅ Evita mixups de versão

### Tessl Framework (Closed Beta)
- 🔒 Mantém agentes "nos trilhos"
- 🔒 Specs como memória de longo prazo
- 🔒 Workflows "vibe-spec" com IA
- 🔒 Requer acesso beta (waitlist em tessl.io)

## Troubleshooting

### "Not authenticated"
```bash
tessl login
# Se falhar, verifique conectividade com internet
```

### "Search failed"
Requer autenticação. Execute `tessl login` primeiro.

### "Failed to initialize login flow"
Problema de conectividade. Verifique:
- Conexão com internet
- Firewall/proxy não bloqueando tessl.io
- Tente novamente mais tarde

## Recursos adicionais

### Documentação oficial
- 🌐 Site: https://tessl.io/
- 📚 Blog: https://tessl.io/blog/
- 🚀 Guia de início: https://tessl.io/docs/

### Artigos e referências
- [How Tessl's Products Pioneer SDD](https://tessl.io/blog/how-tessls-products-pioneer-spec-driven-development/)
- [Understanding SDD - Martin Fowler](https://martinfowler.com/articles/exploring-gen-ai/sdd-3-tools.html)
- [Tessl Framework Launch](https://tessl.io/blog/tessl-launches-spec-driven-framework-and-registry/)

### Comunidade
- 💬 Feedback: `tessl feedback "sua mensagem"`
- 🐛 Issues: GitHub do projeto

---

**Configurado em**: 2026-01-20
**Versão do Tessl**: 0.57.3
**Status**: MCP integrado com Claude Code ✅
