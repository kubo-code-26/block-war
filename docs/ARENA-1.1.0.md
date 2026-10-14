# Block War 1.1.0 — etapas de crescimento

## Atualização de 22/10/2026

O modo Desafio do dia foi removido por solicitação do usuário: botão, página, lógica de partida, modelos específicos de compartilhamento e serviços bwDailyBegin, bwDailyBoard e bwDailyFinish. A Temporada permanece no menu como atalho compacto à direita. Os registros anteriores de publicação abaixo são históricos; as descrições do desafio diário não representam uma funcionalidade disponível na versão atual. O histórico de resultados existente no banco não foi apagado.

## Validação e publicação — 21/10/2026

As etapas 2 a 6 estão implementadas. A compilação debug, os 103 testes Android e o lint terminaram com sucesso (zero erros; 189 avisos de lint permanecem). Os 40 testes Node passaram sem testes ignorados, incluindo a reprodução pelo servidor do replay de 60 peças produzido pelo Kotlin. As prévias de materiais e do cartão foram renderizadas e inspecionadas.

Publicação Firebase concluída: bwRematch, bwDailyBegin, bwDailyBoard, bwDailyFinish, bwSeason, bwEquipBadge e bwPublishSeason; atualizações de bwCreateRoom, bwJoinRoom, bwFindQuickRoom, bwSettleMatch e bwEconomy. Os índices de score e XP foram publicados sem liberar leitura ou escrita direta nos rankings. A página de convites e o assetlinks.json estão publicados em block-war-convites.web.app. Os arquivos anteriores do servidor operacional foram preservados em backup local antes da atualização. A configuração de entrega de ataques não foi alterada.

O APK debug serve à validação técnica e não deve substituir a instalação assinada do teste interno. A geração do AAB de distribuição aguarda a senha da chave existente, informada localmente pelo script de assinatura. A distribuição e os testes de integração em aparelhos ainda estão pendentes; a versão atual da Play Store não foi atualizada automaticamente.

## Convites e revanche

Salas privadas têm botão Convidar amigo. O compartilhamento abre o seletor do Android; nenhuma mensagem é enviada automaticamente. O endereço https://block-war-convites.web.app/?sala=ABCD abre o aplicativo quando a versão instalada possui o novo filtro de App Links. No navegador há botão para abrir o aplicativo, código para entrada manual e acesso ao Google Play. O arquivo assetlinks.json associa o domínio somente ao pacote e certificado da distribuição Google Play.

O convite fica pendente quando chega durante uma partida. A entrada exige confirmação no menu e perfil conectado. A revanche cria uma sala privada, com nova identificação de partida e novas sementes. O primeiro participante solicita e espera; o segundo aceita pelo botão Revanche. Terceiros não entram na sala reservada. O convite de revanche expira após dez minutos. Os resultados e prêmios da partida anterior não são reutilizados.

## Desafio diário

Modo solo de até 60 peças, sem cronômetro e sem CPU. Todas as contas recebem a mesma sequência do período, definido pelo servidor no horário de Brasília. Permite novas tentativas, mantendo apenas o melhor resultado de cada perfil. Não altera BWs, economia ou estatísticas das partidas comuns.

O aplicativo envia os encaixes e confirmações, não uma pontuação aceita por confiança. O servidor reconstrói o tabuleiro e recusa peças inventadas, eventos de adversário, ataques, tentativas incompletas e mais de 60 peças. A pontuação do Kotlin é comparada com o verificador JavaScript por um replay de referência de 60 peças.

A tentativa atual sobrevive à saída da página enquanto sua ViewModel permanecer no aplicativo. Resultados pendentes podem ser reenviados sem duplicar o ranking. Não é prometida restauração da tentativa após encerramento do processo. Há confirmação antes de descartar uma tentativa, preservando o recorde já confirmado. As sessões expiram após três horas e os resultados ficam vinculados ao dia de início.

## Temporadas e coleção visual

### Ajuste de legibilidade

Ao equipar Fundição, Catedral Glacial ou Caverna Prismática, o cenário correspondente aparece no fundo da arena durante a partida. O tabuleiro principal e o mini tabuleiro usam uma superfície escura opaca e grade temática discreta, com brasas, gelo ou cristal nas bordas. A imagem não aparece por trás das peças. A prévia da loja usa o mesmo desenho da grade. Os itens e a seleção de fundo permanecem preservados; ao sair desses três temas, o fundo equipado volta a ser utilizado. As regras de XP permanecem inalteradas.

Três temas mensais alternam: Forja da Arena, Coroa Glacial e Império de Cristal. Há metas de 100, 300, 700 e 1.500 XP. Conclusões online válidas dão 30 XP por vitória ou 10 por derrota, somados às linhas limpas, limitadas a 100 por partida. CPU e abandono não dão XP de temporada. Recibos de partida impedem duplicação. A renovação mensal não remove itens já resgatados.

São 25 itens novos: dez materiais para peças, três tabuleiros, três efeitos e nove itens exclusivos (molduras, estandartes e animações). Os exclusivos exigem resgate; não podem ser adquiridos por zero moedas. Os outros itens permanecem disponíveis na loja, e alguns também fazem parte das recompensas. A aparência não altera colisão, ataque, velocidade ou pontuação.

Os cenários e o atlas de materiais foram criados pelo imagegen embutido. As peças combinam textura, relevo e reflexos desenhados pelo renderizador. Os blocos cinzas continuam distinguíveis pelo acabamento e pelas ranhuras. Texturas são carregadas com amostragem reduzida, RGB565 e cache de quatro imagens. Partículas são limitadas a 96 por emissão e 256 simultâneas. O adversário online vê o material e o tabuleiro equipados no início da partida, validados pelo servidor.

## Escudo em destaque

Na página de conquistas, um escudo desbloqueado pode ser escolhido como destaque ou ocultado. O servidor verifica categoria, nível e meta cumprida. O destaque aparece no perfil, na partida e no resultado. A sala online transporta somente o escudo e a aparência públicos, sem PIN, e-mail ou inventário completo.

## Cartão de resultado

Resultados de CPU, online e desafio diário podem gerar imagem vertical de 900 × 1.600. O compartilhamento é manual pelo seletor do Android. O cartão contém apelido, resultado, estatísticas e escudo selecionado; não contém identificadores de autenticação. O FileProvider dá acesso temporário apenas à pasta de cartões do cache. O diálogo de resultado permite rolagem para alcançar os botões em telas menores.

## Distribuição e teste real

Versão preparada: código 2, nome 1.1.0. O aplicativo precisa de novo AAB com a chave de upload existente. A senha é digitada somente no computador por scripts/Gerar-AAB.ps1. Não substituir a instalação da Play Store por APK debug e não apagar dados para testar estas funções.

Antes de ampliar o teste, validar em dois aparelhos: convite com aplicativo aberto/fechado, confirmação após login, revanche nas duas ordens, adversário que recusa/sai, expiração, repetição de resgates, virada diária e mensal, rejeição de escudo bloqueado, troca de perfil, interrupção da rede e compartilhamento por apps diferentes. A instalação atual da loja não ganha estas telas antes da distribuição do novo AAB.
