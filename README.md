## **dms-search-service**

Disponibiliza a API para realizar pesquisas no DMS.
 
- v1 e v2 adicionam os seguintes itens/features:
	- cache da entity document_category: 5 minutos para document_category. A key do cache é document_category.name
	- cache da entity document_type: 5 minutos para document_type. A key do cache é document_type.name e document_category.name.

- v2 adiciona as seguintes features:
    - controle expiração de documento: permite buscar documentos pelos seguintes escopos: VALID (documentos não expirados), EXPIRED (documentos expirados), ALL (todos documentos), LATEST (ultima versã do documento).
    - controle de acesso: exige token JWS para consumir os resources da versão 2. Valida integridade, autenticidade e expiração.
    

## **swagger**

O swagger da API pode ser acessado em <http://dms-search-service.k8s.dev.bancoagiplan.com.br/swagger-ui.html>

## **RDS**
mysql-dev-dms-document.agiplan.aws.local:3306/dms

- tables:
	- document_category: armazena a categoria do documento. Ex: ac:identificacao, ac:comprovante
	- document_type: armazena os tipos de documento associados a uma categoria especifica. Ex: CPF, RG, CNH, RENDA, RESIDENCIA
	
- cardinalidade:
	**document_category 1 ------------ 0..* document_type**

## **release notes**

- 01/05/2019
    - filtro por escopo do documento
    - controle de acesso
    - configurações em db ao invés de properties
    - uso do vault
    - cache de entities usando ehcache