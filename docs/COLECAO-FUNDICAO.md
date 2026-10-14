# Coleção Fundição

A coleção utiliza o cenário de fundição existente e um renderizador de fogo com línguas curvas em três camadas, oscilação, núcleo luminoso e contorno de brasas. As chamas variam continuamente, sem utilizar a aleatoriedade do motor de partidas.

| Categoria | Item | Acabamento |
| --- | --- | --- |
| Tabuleiro | Fundição | Grade escura, bordas incandescentes e labaredas em movimento; cenário atrás da arena |
| Peças | Obsidiana Fundida e Magma Vivo | Chamas contidas nas bordas das células; cor identificadora preservada |
| Efeitos | Fagulhas de Metal | Mistura de brasas, faíscas e pequenas labaredas |
| Limpeza de linhas | Finalização · Forja da Arena | Frente de queima e brasas ascendentes na linha completada |
| Avatar | Sentinela da Fundição | Elmo metálico, visor incandescente e chamas periféricas |
| Moldura | Moldura · Forja da Arena | Metal aquecido com fogo animado |
| Banner | Estandarte · Forja da Arena | Cenário de forja com chamas em movimento nas bordas |
| Fundo | Coração da Fundição | Cenário existente com acabamento de fogo animado |

O avatar custa 1.800 moedas; o fundo custa 2.400. Os outros IDs, compras e resgates permanecem preservados. Moldura, banner e animação continuam recompensas exclusivas da temporada. Os itens são equipados individualmente, permitindo misturar coleções. A música e as regras de XP não foram modificadas.

Os blocos cinzas de ataque permanecem sem fogo, distinguíveis das peças normais. O fogo do tabuleiro fica na periferia; o centro opaco preserva a leitura. As animações são desenhadas somente enquanto os componentes estão presentes. A queima acompanha os 200 ms já existentes de limpeza e não altera a duração lógica das jogadas.

As prévias da loja e do perfil usam os mesmos renderizadores da partida. A prévia animada exportada para revisão é uma composição desses renderizadores, não uma captura da interface completa do celular. Ainda é necessário conferir fluidez e contraste em aparelho real após distribuir o novo aplicativo.

Validação em 21/10/2026: APK debug compilado; teste de animação passou com movimento entre quadros, centro escuro estável e blocos cinzas sem alteração. Os 40 testes do servidor passaram e o catálogo foi publicado no Firebase. A renderização foi inspecionada e o acabamento ganhou transparência, brilho e pontas suaves. Arquivo de revisão: `app/build/reports/fundicao-animada.gif`. Nenhuma instalação foi feita: o Motorola não apareceu na lista do ADB nesta verificação.
