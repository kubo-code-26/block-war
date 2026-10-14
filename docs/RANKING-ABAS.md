# Ranking com abas

A página mantém o cabeçalho RANKING e oferece Global e Temporada logo abaixo.

- Global mantém a classificação existente por Pontos BW.
- Temporada consulta bwSeason, sem resgatar recompensas, e mostra o ranking de XP do mês vigente, pódio e posição do jogador.
- Os estados de carregamento e erro da temporada são independentes do ranking global. Há nova tentativa e atualização por gesto.
- As duas entradas de temporada compartilham o componente SeasonRanking e o modelo por apelido, mantendo os dados consistentes com a página de temporada.

Validação: RankingTabsTest verifica seleção e retorno das abas, XP e posição mensal, além de erro com nova tentativa. APK de depuração usa a assinatura de teste existente.