# bolt-clientes

CRUD de cadastro de clientes desenvolvido para o processo seletivo do Grupo Bolt.

## Pré-requisitos

- [Docker](https://www.docker.com/) e Docker Compose instalados

## Como rodar

```bash
docker compose up --build
```

A aplicação sobe na porta `8082` junto com o banco PostgreSQL e o Kafka.

## Como rodar os testes

Com os containers no ar:

```bash
docker exec bolt-clientes-app-1 mvn test -f /app/pom.xml
```

## Documentação da API

Acesse o Swagger UI em:

```
http://localhost:8082/swagger-ui/index.html
```

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/clients` | Cadastrar cliente |
| `PUT` | `/clients/{id}` | Atualizar cliente |
| `DELETE` | `/clients/{id}` | Deletar cliente (lógico) |
| `GET` | `/clients` | Listar todos os clientes |
| `GET` | `/clients/{id}` | Buscar cliente por ID |
| `GET` | `/clients/recent` | Últimos 20 clientes cadastrados |
| `GET` | `/health` | Health check |

## Regras de negócio

- Documento (CPF/CNPJ) único por cliente
- Número de instalação único por unidade consumidora
- Endereços consultados automaticamente via [ViaCEP](https://viacep.com.br/)
- Clientes com unidade consumidora em **SP**, **RS** ou **PR** não são aceitos
- Clientes com unidade consumidora em **MG** são publicados no tópico Kafka `analise_cliente_mg`
- Deleção lógica — nenhum cliente é removido fisicamente do banco

## Tecnologias

| Tecnologia | Versão | Uso |
|------------|--------|-----|
| Java | 17 | Linguagem |
| Spring Boot | 3.2.5 | Framework principal |
| Spring Data JPA + Hibernate | - | Persistência |
| Spring Web | - | API REST |
| Spring Kafka | - | Mensageria |
| Spring Validation | - | Validação de entrada |
| PostgreSQL | 15 | Banco de dados |
| Apache Kafka | latest | Broker de mensagens |
| SpringDoc OpenAPI | 2.5.0 | Swagger UI |
| Lombok | - | Redução de boilerplate |
| JUnit 5 + Mockito | - | Testes unitários |
| Maven | 3.9.6 | Build e dependências |
