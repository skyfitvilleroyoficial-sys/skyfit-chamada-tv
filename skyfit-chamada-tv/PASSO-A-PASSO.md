# Sky Fit Chamada nas TVs — Prova de Conceito

Sistema para a recepção chamar o proprietário de um veículo em todas as TVs da unidade, por cima da programação do Indoor.

```
Recepção (navegador)  ──grava──▶  Firebase (tempo real)  ──avisa──▶  App nas TVs (sobreposição)
                                        ▲                                   │
                                        └──────── "estou online" ───────────┘
```

| Pasta | O que é |
|---|---|
| `app/` | App Android das TVs (Kotlin, sem bibliotecas externas) |
| `painel/index.html` | Painel da recepção |
| `firebase/regras-database.json` | Regras de segurança do banco |
| `.github/workflows/build-apk.yml` | Gera o APK automaticamente no GitHub |

---

## Etapa 1 — Firebase (cerca de 10 min)

1. Acesse **console.firebase.google.com** e clique em **Adicionar projeto**. Nome: `skyfit-chamada`. Pode desativar o Google Analytics.
2. No menu, abra **Criação > Realtime Database > Criar banco de dados**. Local: **Estados Unidos**. Modo: **bloqueado**.
3. Copie a **URL do banco** que aparece no topo (ex.: `https://skyfit-chamada-default-rtdb.firebaseio.com`).
4. Na aba **Regras**, apague tudo, cole o conteúdo de `firebase/regras-database.json` e clique em **Publicar**.
5. Abra **Criação > Authentication > Vamos começar**. Em **Método de login**, ative **Anônimo** e **E-mail/senha**.
6. Na aba **Usuários**, clique em **Adicionar usuário**: e-mail da recepção (ex.: `recepcao@skyfit.com.br`) e uma senha forte.
7. Abra **⚙️ Configurações do projeto > Geral > Seus apps**, clique no ícone **Web `</>`**, registre com o nome `painel` e copie o bloco `firebaseConfig`.

## Etapa 2 — Preencher os dados (2 arquivos)

1. **`app/src/main/java/br/com/skyfit/chamada/Config.kt`**: preencha `API_KEY` (o `apiKey` do passo 7) e `DB_URL` (URL do passo 3, sem barra no final).
2. **`painel/index.html`**: no topo do `<script>`, substitua o bloco `firebaseConfig` pelo que você copiou.

Use o Bloco de Notas. Nos dois arquivos, os ids das unidades (`ville-roy`, `buritis`) precisam ser iguais.

## Etapa 3 — GitHub (gera o APK sozinho)

1. Acesse **github.com/new**. Nome: `skyfit-chamada-tv`. Visibilidade: **Public** (necessário para a TV baixar o APK sem login). Clique em **Create repository**.
2. Clique em **uploading an existing file** e arraste **todo o conteúdo** da pasta descompactada. Clique em **Commit changes**.
3. Confira se a pasta **`.github`** subiu. Se não aparecer na lista, clique em **Add file > Create new file**, digite o nome `.github/workflows/build-apk.yml`, cole o conteúdo do arquivo e salve.
4. Abra a aba **Actions**: o processo **Gerar APK** roda por cerca de 5 minutos. Verde = pronto. Vermelho = abra o erro, copie o log e me envie.
5. O APK fica sempre neste link fixo (troque `SEU-USUARIO`):
   `https://github.com/SEU-USUARIO/skyfit-chamada-tv/releases/latest/download/skyfit-chamada.apk`

Dica: crie um link curto (ex.: bit.ly) para digitar na TV com mais facilidade.

## Etapa 4 — Publicar o painel da recepção

1. No repositório, abra **Settings > Pages**.
2. Em **Source**, escolha **Deploy from a branch**, branch **main**, pasta **/(root)**, e clique em **Save**.
3. Em cerca de 1 minuto, o painel estará em:
   `https://SEU-USUARIO.github.io/skyfit-chamada-tv/painel/`

## Etapa 5 — Instalar em 1 TV (cerca de 5 min, só pelo controle)

1. Na **Play Store** da TV, instale o app **Downloader** (by AFTVnews).
2. Em **Configurações > Apps > Acesso especial a apps > Instalar apps desconhecidos**, permita o **Downloader**.
3. Abra o Downloader, digite o link do APK e instale.
4. Abra o **Sky Fit Chamada**. Escolha a unidade, digite o nome da TV (ex.: `Musculação 1`) e clique em **Salvar e ativar**.
5. Clique em **Liberar sobreposição** e marque o **Sky Fit Chamada**.
6. Volte ao app e clique em **Testar aviso**. O aviso deve aparecer por cima da tela inicial.

---

## Roteiro da prova de conceito (o que valida o projeto)

| # | Teste | Resultado esperado |
|---|---|---|
| 1 | Botão **Testar aviso** na TV | Aviso aparece por 10s e some |
| 2 | Com o Indoor rodando, transmitir pelo painel | Aviso aparece **por cima** do Indoor, que continua rodando por baixo |
| 3 | Transmitir com 3 repetições e cancelar após a 1ª | Repetições seguintes não aparecem |
| 4 | Deixar a TV ligada por 1 dia inteiro | TV continua **Online** no painel |
| 5 | Desligar e religar a TV na tomada | Volta a ficar **Online** sozinha, sem abrir o app |
| 6 | Tirar a internet da TV e devolver | Reconecta sozinha em até 1 minuto |

**O teste 4 é o mais importante:** ele mostra se o firmware da TV encerra o app em segundo plano. Se a TV ficar offline sozinha, me avise para aplicarmos a lista branca via ADB.

---

## Limites e pendências conhecidas (antes de ir para produção)

- **Limite do plano gratuito do Firebase:** 100 conexões simultâneas. Uma unidade (cerca de 20 TVs + painéis) cabe com folga; **5 unidades com 20 TVs passam do limite**. A solução é o plano Blaze (pago por uso). Para esse volume de dados, o custo estimado é praticamente zero, mas exige cartão cadastrado.
- **Chave de assinatura do APK está no repositório** (`app/skyfit.keystore`). Em produção, ela vai para GitHub Secrets.
- **Um login único de recepção para todas as unidades.** Em produção, cada unidade terá o próprio login, com permissão só para as próprias TVs.
- **Histórico fica no Firebase.** A integração com Google Sheets (auditoria e relatórios) entra na versão completa.
- **Atualizar o app nas TVs:** basta repetir o passo 3 da Etapa 5. O app instala por cima e mantém as configurações.
