# 02 — Proteção do Firebase e recompensas verificadas

Este pacote contém as correções 3 e 4. A pasta **app** substitui arquivos do projeto Android. A pasta **servidor** é um projeto Firebase separado: não copie essa pasta para dentro de app.

## Antes de aplicar

1. Aplique o pacote 01, compile e teste. Sincronize as recompensas pendentes dos perfis antes de atualizar para 02. Faça uma cópia do projeto e exporte um backup do Realtime Database.
2. Seu projeto está no plano Spark. Para publicar estas Cloud Functions, será necessário mudar para **Blaze**, com faturamento configurado. Nada foi publicado e seu plano não foi alterado durante a preparação. Configure alertas de orçamento; eles não representam um limite automático de gastos.
3. Em Firebase → Authentication → Método de login, habilite **Anônimo**, mantendo Play Games habilitado. O login anônimo identifica o aparelho; o PIN continua protegendo o acesso a perfis antigos.
4. Use Node.js 22 e Java 21 para os testes dos emuladores. Abra um terminal na pasta **servidor** e execute os comandos abaixo.

```powershell
cd functions
npm ci
npm test
cd ..
.\functions\node_modules\.bin\firebase.cmd login
.\functions\node_modules\.bin\firebase.cmd emulators:exec --project demo-block-war --only auth,database,functions "node functions/test-emulator.cjs"
```

## Publicação, na ordem

Reserve uma janela de manutenção sem partidas ativas. O protocolo online mudou: todas as instalações que participarem das novas salas precisam receber a versão 02.

Primeiro publique apenas o servidor no projeto correto:

```powershell
.\functions\node_modules\.bin\firebase.cmd deploy --project block-war-ca797 --only functions:block-war-secure
```

Crie o ranking público dos perfis existentes. O script utiliza credenciais administrativas locais (ADC), nunca uma chave colocada no aplicativo. Com o Google Cloud CLI instalado, execute `gcloud auth application-default login`. Copie a URL exata do seu Realtime Database e execute:

```powershell
$env:BLOCK_WAR_DATABASE_URL = 'COLE_A_URL_HTTPS_DO_SEU_REALTIME_DATABASE'
node functions/backfill-ranking.js
node functions/backfill-ranking.js --apply
```

A primeira chamada apenas simula. A segunda grava somente a projeção pública do ranking e preserva PINs, inventários, BW e blocões.

Depois publique as regras:

```powershell
.\functions\node_modules\.bin\firebase.cmd deploy --project block-war-ca797 --only database
```

Somente então copie **app** para a raiz do projeto Android, aceitando a substituição dos arquivos, compile e instale a versão nova. Publique a atualização para todos os jogadores antes de reabrir as partidas. As regras antigas permitiam acesso total: publicar apenas o servidor sem as novas regras não protege o banco. Publicar somente as regras bloqueia os aplicativos antigos.

## O que mudou

- Novos perfis recebem **100 blocões**. Os saldos existentes são preservados; nenhum saldo antigo de teste é apagado automaticamente.
- BW é usado para patentes/ranking; blocões são usados na loja. Queda manual não pontua. Linhas dão 5/8/12/16 BW, com multiplicadores de combo até 4; vitória soma 20 e derrota desconta 10 sem saldo de perfil negativo. Conclusões válidas pagam 50 blocões online ou 20 contra CPU; abandono não paga moedas.
- Compras, preços, missões e recompensas são calculados pelo servidor. Recibos impedem pagamento duplicado. A lista pública do ranking não contém PINs ou dados privados dos perfis.
- Cada partida recebe uma identificação e sequência de peças do servidor. O replay reconstrói o tabuleiro e verifica os pontos e os ataques. Contra CPU, as decisões do adversário também são verificadas. Isso não substitui uma proteção completa contra automação/bots.
- **A versão 02 precisa de internet para iniciar partidas, inclusive contra CPU.** Os registros ficam persistidos para tentar sincronizar o resultado após uma interrupção. Uma partida antiga, sem sessão emitida pelo servidor, não gera automaticamente uma recompensa confiável; revise manualmente pendências antigas que não foram sincronizadas antes da atualização.
- Salas privadas não entram na busca rápida. A entrada reserva a vaga no servidor. A reconexão preserva o prazo de 30 segundos, sem renová-lo artificialmente.
- A função de entrega de ataques está configurada com `retry: false`, conforme a preferência do usuário. Se uma execução falhar, a política de repetição automática em caso de falha não será acionada.
- Estatísticas permanecem referentes somente a partidas online e a maior pontuação é em BW.

## Verificação no aparelho

Teste criação de perfil (100 moedas), login com PIN certo/errado, vínculo Play Games, troca de perfil, compra/equipar, ranking, missões, partida contra CPU, partida online entre dois aparelhos, retorno antes dos 30 segundos e perda por desconexão após o prazo. Confirme que repetir a sincronização não duplica o prêmio. Os testes locais não substituem este teste no projeto real, que ainda não foi publicado.

Guarde o backup até terminar essa verificação. Voltar somente ao app antigo não é compatível com as regras novas. Não restaure regras públicas de leitura/escrita para resolver um erro de configuração.
