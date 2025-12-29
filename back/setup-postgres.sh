#!/bin/bash

# Script para configurar PostgreSQL com PgVector para DocIntel
# Uso: chmod +x setup-postgres.sh && ./setup-postgres.sh

set -e

echo "================================================"
echo "Configurando PostgreSQL com PgVector"
echo "================================================"

# Cores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Verificar se PostgreSQL está instalado
if ! command -v psql &> /dev/null; then
    echo -e "${RED}PostgreSQL não está instalado!${NC}"
    echo "Instale com: sudo apt install postgresql"
    exit 1
fi

# Verificar se PostgreSQL está rodando
if ! sudo systemctl is-active --quiet postgresql; then
    echo -e "${YELLOW}PostgreSQL não está rodando. Iniciando...${NC}"
    sudo systemctl start postgresql
    echo -e "${GREEN}PostgreSQL iniciado!${NC}"
else
    echo -e "${GREEN}PostgreSQL já está rodando.${NC}"
fi

# Verificar versão do PostgreSQL
PG_VERSION=$(psql --version | grep -oP '\d+' | head -1)
echo -e "${GREEN}Versão do PostgreSQL: ${PG_VERSION}${NC}"

# Verificar se PgVector está instalado
if ! dpkg -l | grep -q "postgresql-${PG_VERSION}-pgvector"; then
    echo -e "${YELLOW}PgVector não está instalado. Instalando...${NC}"
    sudo apt update
    sudo apt install -y postgresql-${PG_VERSION}-pgvector
    echo -e "${GREEN}PgVector instalado!${NC}"
else
    echo -e "${GREEN}PgVector já está instalado.${NC}"
fi

# Configurar banco de dados
echo -e "${YELLOW}Configurando banco de dados...${NC}"

sudo -u postgres psql << EOF
-- Criar banco de dados se não existe
SELECT 'CREATE DATABASE meu_banco'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'meu_banco')\gexec

-- Criar usuário se não existe
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'matheus') THEN
        CREATE USER matheus WITH PASSWORD 'tryhackme';
    END IF;
END
\$\$;

-- Dar permissões
GRANT ALL PRIVILEGES ON DATABASE meu_banco TO matheus;

-- Conectar ao banco
\c meu_banco

-- Dar permissões no schema
GRANT ALL ON SCHEMA public TO matheus;
GRANT ALL ON ALL TABLES IN SCHEMA public TO matheus;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO matheus;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO matheus;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO matheus;

-- Habilitar extensão pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- Verificar extensão
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
EOF

echo -e "${YELLOW}Aplicando schema do banco de dados...${NC}"

# Aplicar o schema do banco
if [ -f "database_schema.sql" ]; then
    sudo -u postgres psql -d meu_banco -f database_schema.sql
    echo -e "${GREEN}Schema aplicado com sucesso!${NC}"
else
    echo -e "${RED}Arquivo database_schema.sql não encontrado!${NC}"
    echo -e "${YELLOW}Execute este script no diretório que contém o arquivo database_schema.sql${NC}"
fi

echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}Configuração concluída com sucesso!${NC}"
echo -e "${GREEN}================================================${NC}"
echo ""
echo -e "Informações da conexão:"
echo -e "  Host:     ${YELLOW}localhost${NC}"
echo -e "  Porta:    ${YELLOW}5432${NC}"
echo -e "  Banco:    ${YELLOW}meu_banco${NC}"
echo -e "  Usuário:  ${YELLOW}matheus${NC}"
echo -e "  Senha:    ${YELLOW}tryhackme${NC}"
echo ""
echo -e "Testar conexão:"
echo -e "  ${YELLOW}psql -h localhost -U matheus -d meu_banco${NC}"
echo ""
echo -e "Ver tabelas:"
echo -e "  ${YELLOW}psql -h localhost -U matheus -d meu_banco -c '\dt'${NC}"
echo ""
echo -e "${GREEN}Pronto para iniciar a aplicação Quarkus!${NC}"

