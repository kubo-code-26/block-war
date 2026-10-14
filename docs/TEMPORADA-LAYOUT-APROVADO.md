# Página de temporada — layout aprovado

A proposta visual aprovada foi transformada em interface Compose, com conteúdo real do Firebase. A página possui capa cinematográfica da Forja, cabeçalho com voltar e atualizar, contador até a virada do mês em Brasília, progresso pessoal, caminho de recompensas, prévia da coleção e pódio.

As linhas de recompensa são compactas e exibem estado resgatado, disponível ou bloqueado. O botão disponível é bloqueado durante a requisição. A confirmação visual de conquista só aparece após o servidor confirmar o resgate. O botão Ver coleção e os atalhos de categoria abrem uma página de prévias, identificando recompensas e itens disponíveis na loja. Compras e equipamentos continuam na loja e no perfil.

O ranking mostra dados reais; vagas abertas ficam identificadas, sem nomes ou XP fictícios. A posição pessoal é mostrada somente quando consta no top 50 retornado. O servidor acrescenta ao pódio apenas os IDs de avatar e moldura públicos, equipados e pertencentes à conta. Nenhum PIN, e-mail ou inventário completo é exposto.

A página permite atualizar pelo ícone ou ao puxar para baixo. Respeita os limites seguros do Android, permite rolagem até o último item e não muda as regras de XP, as metas ou os prêmios. A arte da Forja acompanha o tema vigente; os temas Glacial e Cristal mantêm seus respectivos materiais e cores.

As artes novas estão em `app/src/main/res/drawable-nodpi/season_forge_hero.png` e `avatar_forge_sentinel.png`. O avatar substitui a representação procedural do mesmo item, sem trocar seu ID ou suas compras. Os prompts e o modo integrado de geração estão em TEMPORADA-ARTES-PROMPTS.md. O cache RGB565 foi limitado a 12 MiB para acomodar capa, avatar, cenário e materiais sem expulsões frequentes.

Uma primeira versão foi instalada no Motorola com `adb install -r`, após comparar os certificados públicos e confirmar a mesma assinatura; dados e perfil foram preservados. A versão final passa pela validação dos resgates, abertura da coleção e virada mensal antes da atualização final no aparelho. A distribuição Google Play não é alterada pela instalação local.

Validação final em 22/10/2026: compilação debug aprovada; os três testes da página passaram, cobrindo botão disponível, bloqueio durante carregamento, abertura da coleção, ausência de jogadores fictícios e calendário de Brasília. O teste da coleção animada também passou na etapa anterior. Os 40 testes Node passaram. A função bwSeason foi publicada com sucesso, incluindo somente a aparência pública e validada do pódio. O APK final foi instalado no Motorola com preservação dos dados. A coleção abriu, a rolagem chegou ao ranking e foram capturadas telas reais em `build/season-device-final-top.png`, `build/season-device-final-collection.png` e `build/season-device-final-ranking.png`. Nenhuma compra, resgate ou alteração de progresso foi feita na conta durante a conferência.
