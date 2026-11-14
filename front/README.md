# AstraSoil — Angular 20

Landing page demonstrativa para o projeto AstraSoil, convertida para Angular standalone.

## Requisitos
- Node.js 20+
- npm

## Executar
```bash
npm install
npm start
```
Abra `http://localhost:4200`.

## Build
```bash
npm run build
```

O workspace usa Angular 20, `@angular-devkit/build-angular` 20.3.40 e `zone.js` configurado em `angular.json`.

## Mapa EOX Sentinel-2 cloudless

A seção **Seu campo** usa Leaflet 1.9.4 com uma camada base raster EOX.
Antes, essa área era apenas uma ilustração CSS, sem mapa ou dados geográficos.

Configuração: `src/app/map/eox.config.ts`. O padrão é o mosaico **2025**,
centro ilustrativo em São Paulo e zoom 9. Altere `center` ([latitude, longitude])
e `zoom` para a sua região. A área não representa os limites de uma fazenda.
`year` deve corresponder a uma camada publicada no catálogo; atualize também
as condições de licença ao trocar de mosaico. O endpoint é público, sem chave.

O WMTS REST usa HTTPS, EPSG:3857, matriz `g`, tiles JPEG de 256 px e a ordem
`/{z}/{y}/{x}` (linha antes de coluna), sem inversão TMS:

```
https://tiles.maps.eox.at/wmts/1.0.0/s2cloudless-2025_3857/default/g/{z}/{y}/{x}.jpg
```

A aplicação limita pedidos ao zoom 14 por escolha de qualidade/resolução;
os zooms 15–18 ampliam as mesmas imagens e não acrescentam detalhe.
O mapa permite arrastar, usar teclado e botões de zoom; o zoom por roda fica
desativado para preservar a rolagem da página. Há aviso e botão para repetir
pedidos que falharem. A instância e o observador de tamanho são removidos ao
destruir o componente.

A atribuição fica visível no mapa, com EOX, Copernicus, ano e licença.
**2025 usa CC BY-NC-SA 4.0 para uso não comercial; uso comercial exige licença
EOX.** O mosaico 2016 possui termos distintos (CC BY 4.0) e identificador sem
ano (`s2cloudless_3857`); não basta colocar `year: 2016` nesta configuração.
O serviço público tem uso justo e não oferece SLA. Capturas/exportações com
imagens devem preservar a atribuição visível. O mosaico não é imagem ao vivo
nem uma fonte de indicadores agronômicos.

Documentação consultada em 14/11/2026:
- https://cloudless.eox.at/documentation/usage
- https://cloudless.eox.at/documentation/faq
- https://cloudless.eox.at/documentation/license
- https://tiles.maps.eox.at/wmts/1.0.0/WMTSCapabilities.xml

## Validar

Use Node.js 22.18+ ou 24 para os testes (leitura nativa de TypeScript):

```bash
npm ci
npm test
npm run build
npm start
```

Os testes verificam URL WMTS, ordem dos eixos, atribuição por ano e limites de
zoom, sem depender da rede. Verificação manual: abra `http://localhost:4200`,
vá a **Seu campo**, confira imagens, atribuição, arraste e zoom. Teste também
uma largura de 375 px e bloqueie `tiles.maps.eox.at` no navegador para conferir
o aviso; desbloqueie e clique em **Tentar novamente**.

### Resultado desta integração

- `npm test`: 3 testes passaram.
- `./node_modules/.bin/ngc -p tsconfig.app.json`: passou (TypeScript e templates Angular).
- Consulta ao catálogo WMTS e pedido de tile 9/290/189: HTTP 200, image/jpeg.
- `npm run build`: o processo encerrou com código 134 neste ambiente, sem
  diagnóstico do Angular; repetiu com workers limitados e em desenvolvimento.
  O bundle final e a execução visual no navegador ainda precisam ser validados.
- A instalação reportou 31 vulnerabilidades na árvore de dependências; não
  foram aplicadas atualizações gerais fora do escopo da integração do mapa.

### Alternar mapa base

O switch acima do mapa inicia desligado (EOX). Ligado, usa OpenStreetMap;
desligado, restaura EOX sem alterar centro ou zoom. A atribuição acompanha
a camada ativa. OpenStreetMap usa `https://tile.openstreetmap.org/{z}/{x}/{y}.png`
com cache normal do navegador, sem downloads offline ou pré-carregamento.
Política: https://operations.osmfoundation.org/policies/tiles/

Verificação no navegador: ligar e desligar carregou os tiles dos dois serviços,
atualizou descrição/atribuição e manteve a mesma área visível.

### Cards de rotação e futura API

`src/app/rotations/rotation.model.ts` define `RotationResponse` (`rotations`),
`RotationPlan` (`id`, `title`, `seasons`, `justifications`, `risks`, `needsReview`)
e `RotationSeason` (`year`, `crop`, `icon`, `winterCrop`, que pode ser null).
`rotation.mock.ts` fornece dois exemplos demonstrativos. No componente,
substitua a atribuição de `rotations` por `response.rotations` após integrar e
validar a resposta da API. O template usa `@for`, com IDs estáveis para os cards,
e trata listas vazias. Os textos são interpolados, sem renderizar HTML da API.
Nenhuma API ou cálculo agronômico foi conectado nesta etapa.

### Fotos das culturas

As fotos são arquivos locais em `public/images/crops/`. Autores, fontes e
licenças estão em `credits.json` e na seção expansível de créditos de cada
card. A apresentação aplica enquadramento circular via CSS. O contrato
`RotationSeason` agora recebe `image` (src, author, source, license,
licenseUrl) no lugar de `icon`; `crop-images.ts` centraliza as fotos do demo.

### Contratos futuros: plantas e área climática

Ambos usam mocks locais, sem chamadas aos endpoints futuros:
- `PlantsService.getPlants()`: GET `/v1/plants`, resposta `string[]`; lista
  fornecida em inglês, mais `Solo descoberto`, mantendo os valores do contrato.
- `ClimateAreaService.getArea(latitude, longitude)`: GET `/v1/climate/area`
  com latitude e longitude na query; resposta com `southLatitude`,
  `westLongitude`, `northLatitude`, `eastLongitude`.

O mapa desenha diretamente os limites retornados pelo serviço. O mock usa
células ilustrativas de 0,5° × 0,625° que contêm o ponto; a origem da grade
não foi validada com NASA POWER. Isso substitui o quadrado anterior de
50 × 50 km. A captura Swagger não fornece a URL base, e a API real não foi
testada. Para integrar, substituir `of(mock...)` nos serviços por HttpClient,
configurar a URL base e validar os limites recebidos antes de desenhar.

## Testes unitários com Jest e formatação TypeScript

A suíte atual substitui os antigos testes `node:test`. Usa Jest 30,
`jest-preset-angular` 15 e jsdom, compatíveis com este Angular 20. Execute:

```bash
npm ci
npm test
npm run test:watch
npm run test:coverage
npm run format:ts
npm run format:check
```

`npm test` executa sem Watchman e em um processo. Os testes cobrem as quatro
classes (`AppComponent`, `FieldMapComponent`, `PlantsService`,
`ClimateAreaService`), os mocks de contratos, a configuração EOX e os arquivos
locais de fotos. Leaflet, consultas externas, localização e observador de
redimensionamento são simulados. Isso não valida disponibilidade das APIs,
permissões reais do sistema ou a renderização visual do navegador.

O relatório HTML fica em `coverage/index.html`. Resultado nesta alteração:
7 suítes, 42 testes aprovados; 100% das linhas, 97,53% das instruções,
90,1% das condições e 98,18% das funções do código da aplicação medido.
O ponto de entrada `main.ts` não entra nessa cobertura unitária.

Prettier foi aplicado a todos os `.ts` do projeto, inclusive testes;
dependências, arquivos gerados e cobertura são ignorados. A configuração
está em `.prettierrc.json`. Os testes são excluídos da compilação da aplicação
por `tsconfig.app.json`; `tsconfig.spec.json` configura a compilação deles.

## Integração atual com o backend

A configuração de mocks de plantas/área descrita anteriormente foi substituída.
`api/api.config.ts` define `API_BASE_URL` como
`https://astrasoil-back.onrender.com` (InjectionToken sobrescrevível).
`provideHttpClient()` é registrado em `main.ts`.

- `PlantsService`: GET `/v1/plants`; valida `string[]`, sem catálogo fictício
  em caso de falha. O formulário acrescenta `Bare soil` como condição do terreno.
- `ClimateAreaService`: GET `/v1/climate/area?latitude=...&longitude=...`;
  valida os limites retornados. O mapa usa esses limites diretamente e cancela
  consultas anteriores ao trocar o ponto. Falhas removem o contorno antigo e
  exibem uma mensagem, sem desenhar uma área estimada como fallback.
- Ambos têm timeout de 60 segundos para acomodar a inicialização do Render.
- O backend deve liberar CORS para as origens do frontend (inclusive previews
  da Vercel se forem usados). Foram conferidos HTTP 200 e CORS para o domínio
  de produção e `http://localhost:4202`.
- Rotação continua demonstrativa: o Swagger não possui endpoint de geração.
- `/v1/report/export` foi deliberadamente deixado fora desta integração.

Os testes dos serviços usam `HttpTestingController`; mocks permanecem somente
como fixtures dos testes, sem chamadas reais durante `npm test`.

## Estratégias de rotação

`RotationStrategiesService` envia `POST /v1/rotation-strategies` com latitude,
longitude e histórico (`year`, `plants`). O ponto é propagado pelo mapa ao
formulário; sem ponto ou culturas preenchidas não há envio. O AppComponent
coordena a consulta e passa os resultados ao componente de rotações.

O response é um array com `title`, `years` (`year`, `plant`, `coverCrop`),
`justifications`, `risks`, `reviewPoints`. Não há mais estratégias fictícias
na aplicação. Cobertura nula, ausente, objeto vazio ou nome em branco não
gera bolinha nem flechas. Coberturas válidas mostram nome e motivo entre as
culturas principais. As flechas são alinhadas na mesma linha dos círculos.
Para plantas sem foto cadastrada, usam-se iniciais, sem associar foto de outra
espécie. Exportação PDF continua fora do escopo.

Validação real: o endpoint respondeu HTTP 200 com `[]` ao payload de exemplo
(latitude 42, longitude -93.6, histórico 2026 Wheat/Soybean e 2025 Corn/Cereal
rye). A interface mostra uma mensagem de resultado vazio nesse caso. O
Swagger publicado ainda não listava a rota no momento da consulta.
