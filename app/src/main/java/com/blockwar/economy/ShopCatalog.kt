package com.blockwar.economy

import com.blockwar.model.ShopItem

object ShopCatalog {
    val items = listOf(
        // === BLOCOS / SKINS ===
        // Originais
        ShopItem("neon", "Padrão Neon", "blocos", 0, ""),
        ShopItem("gb", "Game Boy Classic", "blocos", 200, ""),
        ShopItem("cyber", "Cyberpunk Glow", "blocos", 500, ""),
        ShopItem("fireice", "Fogo & Gelo", "blocos", 1000, ""),
        ShopItem("gold", "Ouro Puro", "blocos", 2000, ""),
        // Novos
        ShopItem("skin_padrao", "Blocos Padrão", "blocos", 0, "Os blocos originais"),
        ShopItem("skin_neon_cyber", "Neon Cyber", "blocos", 500, "Blocos brilhantes estilo cyberpunk"),
        ShopItem("skin_pedra_vulcanica", "Pedra Vulcânica", "blocos", 750, "Blocos forjados no magma"),
        ShopItem("skin_gelo_cristalino", "Gelo Cristalino", "blocos", 800, "Blocos translúcidos e gelados"),
        ShopItem("skin_madeira_rustica", "Madeira Rústica", "blocos", 400, "Blocos esculpidos em madeira antiga"),
        ShopItem("skin_ouro_macico", "Ouro Maciço", "blocos", 1500, "Blocos de ouro puro e valioso"),

        // === TABULEIROS ===
        // Originais
        ShopItem("tab_neon", "Padrão Cyber", "tabuleiros", 0, ""),
        ShopItem("tab_matrix", "Matriz Verde", "tabuleiros", 300, ""),
        ShopItem("tab_holo", "Grade Holográfica", "tabuleiros", 600, ""),
        ShopItem("tab_glass", "Vidro Fumê", "tabuleiros", 1200, ""),
        // Novos
        ShopItem("tabuleiro_padrao", "Tabuleiro Padrão", "tabuleiros", 0, "O campo de batalha clássico"),
        ShopItem("tabuleiro_espacial_profundo", "Espaço Profundo", "tabuleiros", 1200, "Batalhe nas estrelas"),
        ShopItem("tabuleiro_templo_antigo", "Templo Antigo", "tabuleiros", 1000, "Ruínas místicas e esquecidas"),
        ShopItem("tabuleiro_cidade_cyberpunk", "Cidade Cyberpunk", "tabuleiros", 1500, "Luzes de neon e arranha-céus"),
        ShopItem("tabuleiro_floresta_magica", "Floresta Mágica", "tabuleiros", 900, "Mundo encantado e misterioso"),
        ShopItem("tabuleiro_arena_gladiador", "Arena do Gladiador", "tabuleiros", 1800, "Onde os campeões se enfrentam"),

        // === AVATARES ===
        // Originais
        ShopItem("avatar_default", "Controle Arcade", "avatares", 0, "Ícone de controle arcade clássico"),
        ShopItem("avatar_cyber_pilot", "Piloto Cyber", "avatares", 300, "Piloto cibernético com visor de néon"),
        ShopItem("avatar_pixel_ninja", "Ninja Pixel", "avatares", 600, "Ninja em pixel art com olhos brilhantes"),
        ShopItem("avatar_mecha_bot", "Mecha Bot", "avatares", 1000, "Cabeça de robô futurista em estilo arcade"),
        ShopItem("avatar_hacker", "Hacker Cyber", "avatares", 1800, "Ilustração cyberpunk com luzes de fundo"),
        // Novos
        ShopItem("avatar_padrao", "Avatar Padrão", "avatares", 0, "O jogador inicial"),
        ShopItem("avatar_robo_ninja", "Robô Ninja", "avatares", 600, "Rápido, letal e metálico"),
        ShopItem("avatar_mago_sombrio", "Mago Sombrio", "avatares", 800, "Mestre das artes ocultas"),
        ShopItem("avatar_ciborgue_futurista", "Ciborgue Futurista", "avatares", 1000, "Metade humano, metade máquina"),
        ShopItem("avatar_dragao_de_fogo", "Dragão de Fogo", "avatares", 1500, "A fúria encarnada"),
        ShopItem("avatar_cavaleiro_fantasma", "Cavaleiro Fantasma", "avatares", 1200, "Espírito vingativo com armadura"),

        // === MOLDURAS ===
        // Originais
        ShopItem("moldura_simple", "Moldura Simples", "molduras", 0, "Borda fina circular simples"),
        ShopItem("moldura_neon", "Moldura Neon", "molduras", 300, "Anel brilhante com efeito de pulso cyan/magenta"),
        ShopItem("moldura_plasma", "Moldura Plasma", "molduras", 600, "Borda animada em estilo chamas de plasma azul/laranja"),
        ShopItem("moldura_cyber_gold", "Moldura Cyber Gold", "molduras", 1200, "Moldura metálica dourada com detalhes brilhantes"),
        // Novos
        ShopItem("moldura_padrao", "Moldura Padrão", "molduras", 0, "Simples e elegante"),
        ShopItem("moldura_chamas_infernais", "Chamas Infernais", "molduras", 700, "Envolvida em fogo ardente"),
        ShopItem("moldura_teia_de_aranha_neon", "Teia de Aranha Neon", "molduras", 900, "Design tecnológico e brilhante"),
        ShopItem("moldura_correntes_de_ferro", "Correntes de Ferro", "molduras", 500, "Para os mais durões"),
        ShopItem("moldura_gelo_congelante", "Gelo Congelante", "molduras", 800, "Fria como a sua vingança"),
        ShopItem("moldura_ouro_cravejado", "Ouro Cravejado", "molduras", 2000, "Exibe sua riqueza para os rivais"),

        // === BANNERS ===
        // Originais
        ShopItem("banner_bot", "Matriz Clássica", "banners", 0, "Animação clássica de minos caindo"),
        ShopItem("banner_overclock", "Overclock Neon", "banners", 300, "Núcleo cibernético com pulso neon e partículas"),
        ShopItem("banner_matrix", "Matriz Verde", "banners", 600, "Chuva digital de código verde estilo ciberespaço"),
        ShopItem("banner_supernova", "Supernova Estelar", "banners", 1200, "Choque magenta/cyan com ondas de energia contínuas"),
        // Novos
        ShopItem("banner_padrao", "Banner Padrão", "banners", 0, "O fundo inicial do seu perfil"),
        ShopItem("banner_explosao_estelar", "Explosão Estelar", "banners", 1000, "Um espetáculo cósmico"),
        ShopItem("banner_tempestade_de_raios", "Tempestade de Raios", "banners", 800, "Poder elétrico incontrolável"),
        ShopItem("banner_aurora_boreal", "Aurora Boreal", "banners", 1200, "Beleza natural e hipnotizante"),
        ShopItem("banner_chuva_de_meteoritos", "Chuva de Meteoritos", "banners", 1500, "Fogo caindo dos céus"),
        ShopItem("banner_portal_dimensional", "Portal Dimensional", "banners", 1800, "Viagem para o desconhecido"),

        // === FUNDOS ===
        // Originais
        ShopItem("space", "Espaço Sombrio", "fundos", 0, ""),
        ShopItem("matrix", "Matrix Code", "fundos", 300, ""),
        ShopItem("sunset", "Sunset Synthwave", "fundos", 600, ""),
        ShopItem("arena", "Arena Pixel Art", "fundos", 1200, ""),
        // Novos
        ShopItem("fundo_padrao", "Fundo Padrão", "fundos", 0, "Fundo escuro clássico"),
        ShopItem("fundo_galaxia_espiral", "Galáxia Espiral", "fundos", 1500, "O infinito espaço"),
        ShopItem("fundo_caverna_de_cristais", "Caverna de Cristais", "fundos", 1200, "Brilho místico subterrâneo"),
        ShopItem("fundo_paisagem_cyberpunk", "Paisagem Cyberpunk", "fundos", 1800, "Visão distante da metrópole"),
        ShopItem("fundo_ruinas_flutuantes", "Ruínas Flutuantes", "fundos", 1000, "Arquitetura antiga no céu"),
        ShopItem("fundo_abismo_oceanico", "Abismo Oceânico", "fundos", 1400, "O mistério das profundezas"),

        // === EFEITOS ===
        // Originais
        ShopItem("sparks", "Faíscas Padrão", "efeitos", 0, ""),
        ShopItem("explosion", "Explosão Pixels", "efeitos", 400, ""),
        ShopItem("plasma", "Fogo / Plasma", "efeitos", 800, ""),
        ShopItem("coins", "Chuva de Moedas", "efeitos", 1500, ""),
        // Novos
        ShopItem("efeito_padrao", "Efeito Padrão", "efeitos", 0, "Pequenas faíscas ao limpar linhas"),
        ShopItem("efeito_explosao_plasma", "Explosão de Plasma", "efeitos", 1200, "Desintegração de energia"),
        ShopItem("efeito_chuva_de_confetes", "Chuva de Confetes", "efeitos", 800, "Celebre cada combo"),
        ShopItem("efeito_impacto_terrestre", "Impacto Terrestre", "efeitos", 1500, "Treme a tela com pedras"),
        ShopItem("efeito_onda_de_choque", "Onda de Choque", "efeitos", 1800, "Expansão sônica ao pontuar"),
        ShopItem("efeito_brilho_celestial", "Brilho Celestial", "efeitos", 2500, "Luz divina e purificadora"),

        // === MUSICAS ===
        // Originais
        ShopItem("arcade", "Arcade Classic", "musicas", 0, ""),
        ShopItem("rush", "Battle Rush", "musicas", 350, ""),
        ShopItem("synth", "Dark Synth Retro", "musicas", 700, ""),
        // Novos
        ShopItem("musica_padrao", "Tema Padrão", "musicas", 0, "A música original do jogo"),
        ShopItem("musica_synthwave_intenso", "Synthwave Intenso", "musicas", 1500, "Ritmo acelerado e retro-futurista"),
        ShopItem("musica_orquestra_epica", "Orquestra Épica", "musicas", 2000, "Grandiosidade para batalhas difíceis"),
        ShopItem("musica_batida_eletronica", "Batida Eletrônica", "musicas", 1000, "Dance enquanto joga"),
        ShopItem("musica_rock_pesado", "Rock Pesado", "musicas", 1200, "Guitarras distorcidas e adrenalina"),
        ShopItem("musica_ambiente_espacial", "Ambiente Espacial", "musicas", 800, "Sons relaxantes da galáxia"),

        // === ANIMAÇÕES ===
        // Originais
        ShopItem("anim_flash", "Destello Neón", "animacoes", 0, "Brilho e colapso rápido"),
        ShopItem("anim_laser", "Corte Láser", "animacoes", 400, "Varredura laser horizontal"),
        ShopItem("anim_glitch", "Disolución Glitch", "animacoes", 800, "Fragmentação e ruído digital"),
        ShopItem("anim_supernova", "Implosión Supernova", "animacoes", 1500, "Contração central e halo de luz"),
        // Novos
        ShopItem("animacao_padrao", "Animação Padrão", "animacoes", 0, "Movimentos básicos"),
        ShopItem("animacao_entrada_triunfal", "Varredura Neon", "animacoes", 2000, "Varredura luminosa ao eliminar linhas"),
        ShopItem("animacao_vitoria_espetacular", "Estilhaços Dourados", "animacoes", 2500, "Estilhaços dourados na linha eliminada"),
        ShopItem("animacao_derrota_dramatica", "Fragmentação Rubra", "animacoes", 1500, "Fragmentação rubra ao eliminar linhas"),
        ShopItem("animacao_level_up_brilhante", "Cristais de Luz", "animacoes", 1800, "Cristais brilhantes na eliminação de linhas"),
        ShopItem("animacao_combo_devastador", "Raio Devastador", "animacoes", 2200, "Raio que percorre a linha eliminada")
    )

    fun find(id: String): ShopItem? = items.firstOrNull { it.id == id }
}