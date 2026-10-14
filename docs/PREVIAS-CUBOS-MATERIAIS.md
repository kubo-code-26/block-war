# Prévias de materiais em perspectiva

As prévias de skins na loja, coleção e caminho da temporada usam três cubos separados em perspectiva, substituindo a silhueta em T. Cada face recebe a textura do material existente, iluminação e sombra. Obsidiana e Magma mostram fissuras com brilho animado.

A renderização fica em MaterialCubePreview e é compartilhada por CosmeticThumbnail e BlockSkinPreview. As células do tabuleiro continuam com a renderização de jogo própria; a lógica das peças, equipamentos, compras e recompensas não foi alterada.

MaterialCubePreviewTest gera build/reports/material-cubes-preview.png com doze amostras para revisão visual. A validação inclui materiais distintos, enquadramento, renderização existente das células e navegação da loja/temporada.