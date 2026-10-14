# Remoção do Desafio do dia — 22/10/2026

Por solicitação do usuário, foram removidos o atalho da tela inicial, a página, a lógica de tentativa, o registro específico de resultados e as variantes de compartilhamento do modo. O atalho Temporada continua compacto à direita. Tarefas diárias do sistema de missões continuam independentes desse modo.

Os callables bwDailyBegin, bwDailyBoard e bwDailyFinish foram excluídos do Firebase. A consulta posterior confirmou ausência dos três e disponibilidade de bwSeason. Foram retirados os respectivos módulos e testes específicos; a validação online continua rejeitando sessões de modos diferentes de CPU e ONLINE para recompensas comuns. O histórico existente no banco não foi apagado. As fontes anteriores foram preservadas em backup local de auditoria.

Compilação debug e testes Android concluídos com sucesso; os 37 testes restantes do servidor também passaram. A atualização do Motorola ficou pendente porque o aparelho desconectou durante a preparação; foi solicitado que o usuário o reconecte. A versão local está em app/build/outputs/apk/debug/app-debug.apk.

Após a reconexão, a atualização foi instalada com sucesso no Motorola 0083058528 usando install -r, preservando os dados. A conferência da tela inicial confirmou ausência do Desafio do dia e presença dos botões Temporada, Jogar vs CPU e Jogar online 1v1. Captura: build/home-without-daily-motorola.png. Todos os 105 testes Android passaram antes da instalação.
