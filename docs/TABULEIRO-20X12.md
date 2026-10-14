# Tabuleiro de 20 linhas por 12 colunas

O cliente utiliza PlayerState.BOARD_ROWS=20 e BOARD_COLS=12. Spawn, CPU, colisões, ondas de ataque, limpeza de linhas, minitabuleiro, tutorial, grade temática e proporção de tela acompanham as dimensões. As células continuam quadradas.

O backend registra boardCols na sala e na sessão verificada. Novos clientes informam 12; a ausência do campo mantém o protocolo anterior de 10 colunas. Busca rápida, entrada privada e revanche exigem tamanhos compatíveis. O verificador deriva os limites do tabuleiro da sessão; o cliente não escolhe dimensões durante o replay. Somente 10 e 12 colunas são aceitas.

Funções atualizadas: bwCreateRoom, bwJoinRoom, bwFindQuickRoom, bwRematch, bwBeginMatch, bwSendAttack, bwFinishRoom e bwSettleMatch.

Validação: 43 testes do backend; testes do Android cobrem as duas colunas novas e limpeza apenas após preencher 12 células. Uma partida de 68 peças registrada pelo Android foi reconstruída pelo servidor com o mesmo resultado. As 36 decisões de CPU exportadas pelo Android corresponderam ao cálculo independente do servidor nas três dificuldades.

Jogadores online precisam usar versões com o mesmo tamanho de tabuleiro. Perfis, saldos e itens não são reiniciados.