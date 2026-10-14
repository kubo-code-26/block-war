# Tutorial jogável — etapa 1

Três desafios curtos, sem cronômetro: deslocar e encaixar uma peça de dois blocos, girar uma peça de três blocos para preencher uma abertura vertical e limpar uma linha com a peça de quatro blocos para gerar cinco blocos de ataque.

O treino usa BlockWarEngine para colisões, rotação, sombra, pouso, limpeza e quantidade de ataque. O tabuleiro é independente da ViewModel das partidas: não chama o servidor, não envia resultados e não altera saldo, missões, estatísticas ou conquistas. Erros reiniciam o desafio atual, com orientação para tentar novamente.

A apresentação aparece uma vez por instalação depois de entrar em um perfil, com opções de começar e pular. Pular também registra a apresentação como vista. O tutorial permanece acessível em Ajustes → Aprenda a jogar → Tutorial jogável. A conclusão é guardada localmente e sair de uma repetição não apaga uma conclusão anterior.

Há botões de treino, toque para girar, deslize lateral para mover e deslize rápido para baixo para soltar. O botão de voltar e o gesto de voltar do Android fecham o treino. Etapa e posição são preservadas pelo estado salvável do Compose. Cabeçalho, área segura e rolagem seguem o restante do aplicativo; os controles podem ocupar mais de uma linha com fonte ampliada.

As etapas futuras continuam separadas: convite de amigos/revanche, desafio do dia, temporadas com coleção visual mais realista, exibição de escudos e compartilhamento dos resultados. Esta implementação não anuncia nenhuma dessas funções como disponível.

A versão Android preparada continua 1.0.1 (código 2), ainda não distribuída. O tutorial precisa entrar no próximo AAB assinado; não foi instalado APK debug sobre a versão da Play Store.

Validação: suíte completa Android com 97 testes aprovados. Depois do ajuste final de rolagem, os nove testes do tutorial foram executados novamente e passaram, incluindo percurso pelos botões, mover/girar/soltar por gestos, visibilidade das instruções ao avançar, limites do tabuleiro, repetição sem duplicar progresso e persistência local. assembleDebug e lintDebug concluíram com BUILD SUCCESSFUL. Ainda falta a conferência da versão assinada em aparelhos reais.
