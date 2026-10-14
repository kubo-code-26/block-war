# Caminho completo da coleção Fundição

O caminho mostra nove prévias individuais distribuídas por sete metas. Os índices e recompensas das quatro metas anteriores permanecem intactos: 100 XP moldura, 300 XP Obsidiana, 700 XP Fagulhas, 1500 XP tabuleiro + banner + finalização. O pacote de 1500 XP aparece em três linhas e mantém um único resgate conjunto.

Novas metas anexadas: 2000 XP avatar Sentinela, 2500 XP fundo Coração da Fundição e 3500 XP Magma Vivo. Itens já comprados não são duplicados nem removidos. Outros temas mantêm sua definição anterior.

O servidor valida o índice contra o número real de metas da temporada, mantém os resgates idempotentes e verifica o XP antes de conceder os itens. Somente bwSeason precisa ser atualizado.

Layout: moldura metálica com cantos recortados, brilho ambiental atrás do conteúdo, trilha conectada, prévias maiores e botões distintos para resgatado, disponível e bloqueado. Teste em 320 dp cobre os nove itens e preservação dos índices de resgate.
## Verificação de publicação

Os 38 testes do servidor e 111 testes do aplicativo passaram. O APK foi enviado ao Google Drive como Block-War-fundicao-9-recompensas.apk.

A publicação isolada de bwSeason encontrou falhas de inicialização do Cloud Run nas revisões 00003 e 00004. A tentativa com Firebase CLI terminou com sucesso na revisão 00005-vix; o módulo season.js extraído da imagem ativa conferiu, por SHA-256, com a fonte testada e continha as metas adicionais. Não foi demonstrado defeito no código ou no Node.js. Um teste temporário do Node 22.23.3 também concluiu; as variáveis originais de build foram restauradas em seguida para preservar a política automática de runtime.