# AstroSoil

Monorepo Nx para o projeto NASA Space Apps Challenge 2026.

- `front/`: Angular 20, Leaflet, Jest e Prettier.
- `back/`: Spring Boot, Java 27 e Maven Wrapper.
- `tools/`: lançadores multiplataforma.

## Frontend

Requisitos: Node.js 22.12+ (linha 22) ou 24 e npm 10+. Instale uma vez na raiz:

```bash
npm ci
npm start
```

Abra http://localhost:4200. Caso essa porta já esteja em uso:

```bash
npm start -- --port=4201
```

O frontend usa mocks locais para plantas, área climática e rotações. Não precisa
iniciar o backend. Tiles EOX/OpenStreetMap e busca Photon precisam de internet;
geolocalização depende de permissão do navegador e do sistema operacional.

```bash
npm run build
npm test
npm run front:test:coverage
npm run front:typecheck
npm run format:check
npm run format:ts
```

O build de produção fica em `dist/front/browser/`; a cobertura em
`coverage/front/index.html`. `npm start` é o servidor de desenvolvimento.
Para produção, publique o diretório do build em um servidor estático com fallback
para `index.html`; não use o servidor de desenvolvimento como hospedagem.

Comandos Nx equivalentes:

```bash
npx nx serve front
npx nx build front
npx nx test front
npx nx run front:test-coverage
npx nx run front:typecheck
```

A configuração Angular permanece em `front/angular.json`, e os targets em
`front/project.json`, seguindo o executor `nx:run-commands` usado no backend.
O lançador limita workers de build a 2; `NG_BUILD_MAX_WORKERS` pode sobrescrever.
Não há instalação separada em `front/`: o lockfile e as dependências são da raiz.
Detalhes de mapas, licenças, mocks e testes: [front/README.md](front/README.md).

## Backend

Instale Java **27** e configure `JAVA_HOME` antes de executar os comandos
existentes. O Maven Wrapper baixa a versão Maven definida pelo backend:

```bash
npm run back:serve
npm run back:build
npm run back:test
```

A porta padrão é 8080. O perfil padrão é `demo`, conforme a configuração do
backend. Em dois terminais, use `npm run back:serve` e `npm start` para executar
os dois projetos. As APIs reais ainda não estão conectadas ao frontend.

## Integração local

A branch `feature/front-end` contém a integração como alterações locais para
revisão. Nenhum commit ou push foi realizado nesta preparação.

### Validação desta integração

- `npm ci`: instalação pelo lockfile da raiz.
- `npm run build`: build Angular de produção pelo Nx.
- `npm run front:test:coverage`: 7 suítes, 42 testes aprovados.
- `npm run front:typecheck` e `npm run format:check`: aprovados.
- Servidor frontend validado na porta 4201 para preservar outro servidor na 4200.
- O cache em disco do Angular foi desativado em `front/angular.json` porque
  o LMDB nativo abortou neste Mac/Node 24. O cache de tarefas Nx continua ativo.
- Backend não validado neste computador: o projeto requer Java 27 e o Java
  instalado é 17. As fontes e os targets do backend não foram alterados.
