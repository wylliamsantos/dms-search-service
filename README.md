## dms-search-service

Serviço responsável por expor consultas aos documentos armazenados no DMS. Toda a leitura agora ocorre diretamente no MongoDB, reutilizando os mesmos modelos do `dms-document-service`.

### Build & Test

```bash
./gradlew compileJava
./gradlew test
./gradlew bootRun
```

Certifique-se de ter MongoDB disponível com a base `dms` (mesmo schema do document-service).

### API disponível

#### `POST /v1/search/byCpf`

Payload esperado:
```json
{
  "cpf": "12345678900",
  "documentCategoryNames": ["ac:identificacao"],
  "searchScope": "ALL",    
  "versionType": "MAJOR"
}
```

- `cpf`: obrigatório, filtra diretamente pelo índice (`cpf`,`category`).
- `documentCategoryNames`: categorias permitidas (service filtra pelas que pertencem ao grupo PERSONAL).
- `searchScope`: controla vencimento (ALL/VALID/EXPIRED/LATEST).
- `versionType`: MAJOR/MINOR/ALL; ALL retorna a versão mais recente independente do tipo.

A resposta é uma `Page<EntryPagination>` com dados estruturados para consumo pelo frontend.

#### Endpoints removidos

Os antigos `/byAuthor`, `/byMetadata`, `/byQuery` foram eliminados (eram CMIS/Alfresco). Qualquer necessidade futura deve ser reimplementada via Mongo.

### CORS

Configurável por propriedades (`dms.cors.allowed-origins`, `DMS_CORS_ALLOWED_ORIGINS`). Default permite `http://localhost:5173` para integração com o frontend.

### Observabilidade

- Actuator `/actuator/health`, `/actuator/prometheus`
- Ehcache para cache de `documentCategory`

### Execução local

1. Exportar Java 21 (`JAVA_HOME`).
2. Subir Mongo com os metadados do DMS.
3. `./gradlew bootRun` (porta padrão 8081).
