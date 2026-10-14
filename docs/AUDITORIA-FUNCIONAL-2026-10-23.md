# Auditoria funcional do Block War — 23/10/2026

## Conclusão

Os cenários normais verificados passaram: tabuleiro 20×12, movimentos, CPU, ataques, combos, recompensas, compras, temporada e reconexão simulada. Há uma pendência importante na recuperação de ataques após falhas transitórias. A revisão não equivale a uma certificação de funcionamento em todos os aparelhos ou redes.

## Evidências

- Android: 114 testes, zero falhas; assembleDebug e lintDebug concluídos. Lint final: zero erros e 186 avisos. Os grupos principais são recursos não usados (67), localização de ícones (42), sugestões de Kotlin/KTX (24), dependências (19), versões disponíveis (10), SharedPreferences (7), ícones duplicados (5) e formato de ícone (5).
- Servidor: 47 testes, zero falhas. Foram adicionados testes de entrega duplicada/falha transitória e uma matriz de 18 combinações de premiação.
- Integração local: Auth, Realtime Database e Cloud Functions no projeto exclusivo demo-block-war, com dois usuários fictícios. Ataque real de cinco blocos produzido por replay válido, entrega única, repetição de requisição, resultado da sala, BW, moedas, XP, abandono, resgates, compra e reconexão passaram.
- No cenário integrado, o jogador encerrou uma linha em 12 colunas e gerou cinco blocos de ataque. Repetir o envio não duplicou a fila. O vencedor recebeu 50 moedas e os BW verificados mais 20; repetir o pedido de prêmio inclusive em paralelo não pagou novamente. A temporada recebeu 31 XP (30 da vitória e uma linha). O jogador que abandonou não recebeu moedas ou XP e seu saldo de BW não ficou negativo.
- Todos os itens elegíveis da temporada puderam ser resgatados uma vez. Compra do item gb cobrou 200 moedas do catálogo apenas uma vez, mesmo repetindo o identificador da operação. Índice de recompensa inválido foi recusado.
- Reconexão simulada após um segundo manteve a partida. Desconexão simulada além de 30 segundos confirmou derrota.
- Metadados de produção: bwBeginMatch, bwSendAttack, bwDeliverAttack, bwFinishRoom, bwSettleMatch, bwRoomPresence, bwRoomTimeout, bwEconomy, bwMissions, bwSeason, bwPublishRanking e bwPublishSeason estavam ACTIVE. Não foram criados perfis nem executadas partidas/compras na produção.

## Regras confirmadas

| Evento | BW | Blockoins | XP de temporada |
|---|---|---|---|
| Vitória online | Pontos verificados +20 | 50 | 30 + linhas, até 100 de bônus |
| Derrota online concluída | Pontos verificados −10 | 50 | 10 + linhas, até 100 de bônus |
| Vitória contra CPU | Pontos verificados +20 | 20 | 0 |
| Derrota contra CPU concluída | Pontos verificados −10 | 20 | 0 |
| Abandono/desconexão com derrota | −10, sem creditar pontos da partida | 0 | 0 |

O saldo do perfil não fica negativo. Missões concedem valores adicionais quando resgatadas, portanto o total de moedas pode ser maior que o prêmio-base da partida.

Uma, duas, três ou quatro linhas dão respectivamente 5, 8, 12 ou 16 pontos-base e blocos de ataque. O combo multiplica os pontos até ×4; ele não multiplica a quantidade de blocos enviada. Uma linha completada pelo pouso de blocos cinza é removida, mas não gera um novo ataque ou pontuação de limpeza do jogador, conforme a regra já escolhida para o jogo.

## Correção feita nesta auditoria

O lint encontrou chamadas de java.time incompatíveis com o Android 7 (API 24/25), aceito pelo minSdk do app. Ativei core library desugaring com desugar_jdk_libs 2.1.5, já disponível no ambiente, sem elevar a versão mínima do Android. Compilação, testes e lint passaram após a correção. O APK de teste mantém a assinatura anterior. A correção depende de instalar o novo APK; não houve envio à Play Console.

## Pendência importante: recuperação de ataques

1. Em FirebaseRealtimeManager.sendAttackToOpponent, uma falha da chamada bwSendAttack é registrada no log, sem uma fila própria de reenvio. Uma limpeza futura pode reenviar o total acumulado, mas isso não garante a recuperação do último ataque se a partida terminar antes.
2. bwDeliverAttack está com retry:false; os metadados publicados confirmam RETRY_POLICY_DO_NOT_RETRY. Um teste com falha simulada de banco mostrou que a mensagem permanece na outbox sem recibo e sem entrega ao adversário. A invocação posterior a entrega corretamente, mas a função não é repetida automaticamente. Essa configuração anterior foi preservada.
3. Repetir bwSendAttack depois de consumir o total não altera necessariamente a mensagem pendente, portanto não é um mecanismo garantido para disparar novamente a entrega.

Prioridade recomendada: confirmação de envio/entrega e recuperação controlada/idempotente de pendências, com tentativas limitadas e sem duplicar ataques. Não alterei a política de repetição da produção durante esta auditoria.

## Limites e teste manual restante

- Não foram usadas duas instalações físicas para reproduzir perda de Wi-Fi/dados móveis, troca de rede, suspensão ou encerramento do processo.
- A autenticação real do Play Games e as credenciais da Play Store não foram refeitas. As autenticações do teste integrado são fictícias e locais.
- Emuladores usam Node 24 do computador, enquanto produção usa Node 22. Os testes de integração verificam a lógica e as regras no ambiente local; o estado ACTIVE confirma disponibilidade declarada, não todas as condições reais de rede.
- Android 7 precisa de teste em aparelho/emulador dessa versão para comprovar a experiência completa após a correção, além da verificação estática e do empacotamento.
- Os avisos do lint permanecem para manutenção; não representam 186 falhas de execução.

## Sistema de amigos sugerido

É uma boa evolução para facilitar partidas recorrentes com pessoas conhecidas. Eu implementaria após melhorar a recuperação de ataques:

1. Código público do jogador e busca por apelido; enviar, aceitar e recusar pedido.
2. Lista de amigos com status online/ocupado e remover/bloquear.
3. Convite para sala privada e revanche aproveitando o fluxo existente.
4. Ranking entre amigos, usando BW e temporada sem criar outra moeda.

A entrada pode ficar em Perfil → Amigos, com um atalho na tela inicial. A amizade deve pertencer ao perfil do jogador, para continuar após troca de celular ou vínculo ao Google Play Games. Esta auditoria não implementou o sistema de amigos.

## Reprodução

- Android: gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline --no-configuration-cache --max-workers=2
- Unidade do servidor: node --test server/functions/test/*.test.js
- Integração: copiar functions/test-game-audit.cjs ao servidor operacional que contém as dependências e executar, na raiz desse servidor, firebase emulators:exec --project demo-block-war --only auth,database,functions "node functions/test-game-audit.cjs". O script exige host de emulador local; não deve ser executado contra produção.

Logs: build/audit-server-20261023.log, build/audit-game-fixed-20261023.log e build/audit-emulator-20261023.log.