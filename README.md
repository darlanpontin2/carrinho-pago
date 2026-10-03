# carrinho-pago

Módulo backend de carrinho de visitante para integração ao RonyLiboni/rony-ecommerce.

## Escopo

Este repositório contém somente os arquivos adicionais do módulo, não uma cópia
do ecommerce. Não é uma aplicação independente e não contém um pom.xml próprio.

Base analisada: RonyLiboni/rony-ecommerce, commit
5eaf5143fbc5f23da3466c4f3f8a796e910e6abe.

Reutiliza Java 17, Spring Boot 3.1.0, Spring MVC, Jakarta Validation, JPA,
EntityManager, MySQL, Flyway e as entidades de produto da aplicação.
Não adiciona dependências Maven.

Nenhum arquivo existente do projeto-base precisa ser substituído.
As versões originais foram mantidas por compatibilidade, não por uma avaliação
de segurança ou recomendação de uso em produção.

## Integração

Execute em um diretório de trabalho, com Java 17, Git e Maven instalados:

```bash
git clone https://github.com/RonyLiboni/rony-ecommerce.git ecommerce-integrado
git -C ecommerce-integrado checkout 5eaf5143fbc5f23da3466c4f3f8a796e910e6abe
git clone https://github.com/darlanpontin2/carrinho-pago.git modulo-carrinho
cp -R modulo-carrinho/src/main/. ecommerce-integrado/src/main/
cp -R modulo-carrinho/src/test/. ecommerce-integrado/src/test/
cd ecommerce-integrado
mvn -Dtest=CartServiceImplTest,CartSessionFilterTest test
```

A cópia acima é local. Não altera nem publica mudanças no repositório original.

Configure uma instância MySQL 8.0.16 ou superior e um banco exclusivo para teste.
Não execute a migration inicialmente em produção. Faça backup e valide primeiro
em homologação.

Exemplo de execução local por HTTP:

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/rony-ecommerce'
export SPRING_DATASOURCE_USERNAME='ecommerce'
export SPRING_DATASOURCE_PASSWORD='substitua-localmente'
export CART_PUBLIC_ORIGIN='http://localhost:8080'
export CART_COOKIE_SECURE=false
mvn spring-boot:run -Dspring-boot.run.profiles=cart
```

Em produção, use HTTPS, CART_COOKIE_SECURE=true e CART_PUBLIC_ORIGIN com a origem
pública exata, sem barra final. O perfil cart deve permanecer ativo.
Não armazene credenciais no Git.

## Rotas

| Método | Rota | Resultado |
| --- | --- | --- |
| GET | /api/carrinho | Carrinho completo; inicializa sessão e fornece token CSRF |
| POST | /api/carrinho/items | Adiciona ou incrementa; retorna 201 |
| PUT/PATCH | /api/carrinho/items/{produto_id}/quantidade | Define quantidade absoluta |
| DELETE | /api/carrinho/items/{produto_id} | Remove produto do carrinho |
| DELETE | /api/carrinho/clear | Limpa itens e desconto geral |

O identificador das rotas de item é o ID do produto, não o ID interno da linha.
Diminuir para zero não é permitido; use DELETE.
Limpar carrinho vazio é uma operação válida e idempotente.

Campos do POST: produto_id e quantidade, ambos números JSON inteiros positivos.
Quantidade máxima por produto: 1000000. Limite: 100 produtos distintos.

Respostas incluem quantidade_total_itens e quantidade_itens como alias.

## Sessão e CSRF

1. Faça GET /api/carrinho.
2. Guarde o cookie de sessão e o header X-CSRF-TOKEN.
3. Envie ambos em POST, PUT, PATCH e DELETE.
4. Reutilize o cookie nos próximos GETs.

O proprietário é gerado no servidor e armazenado na sessão. Não é aceito por
JSON, query string ou header do cliente. id_carrinho não é o ID da sessão Servlet.

O frontend deve ser servido pela mesma origem pública da API, por exemplo usando
um reverse proxy. O módulo rejeita origens diferentes; não depende do CORS
permissivo existente no projeto-base.

O perfil cart configura cookies HttpOnly, Secure e SameSite=Strict, rastreamento
somente por cookie e expiração por 30 minutos de inatividade.

Exemplo com curl:

```bash
curl -sS -D headers.txt -c cookies.txt http://localhost:8080/api/carrinho
TOKEN=$(awk 'tolower($1)=="x-csrf-token:" {gsub("\r","",$2); print $2}' headers.txt)
curl -i -b cookies.txt \
  -H "X-CSRF-TOKEN: $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"produto_id":1,"quantidade":2}' \
  http://localhost:8080/api/carrinho/items
```

## Estoque e descontos

cart_product_offers é a fonte de estoque disponível e desconto unitário do módulo.
A migration cria ofertas com estoque ZERO para produtos existentes.
Não inventa estoque nem permite ao visitante alterar preços ou descontos.

Após cadastrar produtos, o backend administrativo ou integração de estoque deve
manter essa tabela. Exemplo administrativo para um produto existente de ID 1:

```sql
INSERT INTO cart_product_offers (product_id, stock, unit_discount)
VALUES (1, 20, 5.00)
ON DUPLICATE KEY UPDATE stock = 20, unit_discount = 5.00;
```

O desconto por item na resposta é desconto unitário multiplicado pela quantidade.
shopping_carts.order_discount representa um desconto geral FIXO, não percentual.
Somente processos internos confiáveis devem atualizar esse campo.
Não existem endpoints públicos para definir descontos.

O desconto geral é aplicado depois dos descontos por item e limitado ao subtotal.
DELETE /clear e a remoção do último item zeram o desconto geral.

Nome, foto, preço e desconto unitário são snapshots renovados quando o item é
adicionado ou sua quantidade é atualizada. GET não atualiza preços silenciosamente.

Adicionar ao carrinho NÃO reserva nem baixa estoque. O checkout precisa revalidar
preço, promoções e estoque e realizar a baixa atômica antes de confirmar a compra.
Checkout e pagamento não fazem parte deste módulo.

## Consistência e limites operacionais

As operações usam transações e bloqueio pessimista do carrinho.
A inicialização utiliza upsert MySQL para suportar criação concorrente.
A restrição única (cart_id, product_id) impede duplicações no banco.
A validação de estoque considera a quantidade acumulada no carrinho.

O rate limit é de 120 requisições por minuto POR SESSÃO, configurável por
CART_REQUESTS_PER_MINUTE. Requisições rejeitadas por CSRF também contam.
Não é proteção global contra abuso: criar novas sessões permite novos limites.
Para publicação na internet, aplique limites adicionais no gateway/reverse proxy.

A implementação de sessão e rate limit desta entrega é para UMA instância.
Sessões não são recuperadas após perda da sessão ou reinício sem persistência
configurada. Os registros do carrinho permanecem no banco, mas não são entregues
a outro visitante. Defina uma política operacional de expurgo de carrinhos órfãos.

Para múltiplas instâncias, adapte sessão e rate limit para armazenamento
compartilhado antes de escalar. Locks do banco continuam protegendo os carrinhos,
mas não substituem essa adaptação.

## Erros

400: entrada inválida.
403: origem não autorizada ou CSRF inválido.
404: produto inexistente ou item fora do carrinho atual.
409: estoque insuficiente, oferta ausente, configuração monetária inválida ou
conflito de persistência.
429: rate limit excedido, com Retry-After.
500: falha inesperada, sem exposição de detalhes internos.

Formato: sucesso=false, mensagem e dados.

## Testes e status de validação

Incluídos testes unitários para valores monetários, acúmulo, estoque, alterações
de quantidade, remoção, carrinho vazio, isolamento, CSRF, origem e rate limiting.

Os testes não foram executados durante a geração.
Não há alegação de compilação ou validação de integração concluída.

Antes de produção, execute também testes reais em MySQL para migration,
concorrência, rollback, constraints, criação simultânea de carrinho e integração
HTTP completa com cookies. Os testes fornecidos usam mocks e não cobrem essas
garantias de infraestrutura.
