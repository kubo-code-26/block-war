package com.blockwar.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockwar.model.Cell
import com.blockwar.model.PlayerInventory
import com.blockwar.model.ShopItem

// Paleta "Neon Grid Battle" (Stitch)
private val ShBackground = Color(0xFF121318)
private val ShSurfaceLowest = Color(0xFF0D0E13)
private val ShSurfaceLow = Color(0xFF1A1B21)
private val ShSurface = Color(0xFF1E1F25)
private val ShSurfaceHigh = Color(0xFF292A2F)
private val ShOnSurface = Color(0xFFE3E1E9)
private val ShOnSurfaceVariant = Color(0xFFB9CACB)
private val ShOutlineVariant = Color(0xFF3B494B)
private val ShCyan = Color(0xFF00F0FF)
private val ShOnCyan = Color(0xFF006970)
private val ShPurple = Color(0xFFCF5CFF)
private val ShPurpleSoft = Color(0xFFECB2FF)
private val ShLime = Color(0xFF39FF14)
private val ShCoin = Color(0xFFFFB300)

// A raridade é só visual: deduzida do preço do item (o jogo não tem esse campo)
private enum class ShRarity(val label: String, val color: Color) {
    COMUM("COMUM", Color(0xFF4ADE80)),
    RARA("RARA", Color(0xFF60A5FA)),
    EPICA("ÉPICA", Color(0xFFCF5CFF)),
    LENDARIA("LENDÁRIA", Color(0xFFFFB020))
}

private fun rarityOf(price: Int): ShRarity = when {
    price <= 300 -> ShRarity.COMUM
    price <= 700 -> ShRarity.RARA
    price <= 1200 -> ShRarity.EPICA
    else -> ShRarity.LENDARIA
}

private fun categoryIcon(key: String): ImageVector = when (key) {
    "blocos" -> Icons.Default.GridView
    "tabuleiros" -> Icons.Default.Dashboard
    "avatares" -> Icons.Default.AccountCircle
    "molduras" -> Icons.Default.CropSquare
    "banners" -> Icons.Default.Flag
    "fundos" -> Icons.Default.Wallpaper
    "efeitos" -> Icons.Default.AutoAwesome
    "musicas" -> Icons.Default.MusicNote
    else -> Icons.Default.Bolt
}

private fun categoryLabel(key: String): String = when (key) {
    "blocos" -> "Skin de blocos"
    "tabuleiros" -> "Tabuleiro"
    "avatares" -> "Avatar"
    "molduras" -> "Moldura de perfil"
    "banners" -> "Banner animado"
    "fundos" -> "Fundo de tela"
    "efeitos" -> "Efeito ao limpar linhas"
    "musicas" -> "Trilha sonora"
    else -> "Animação de linha"
}

private fun formatCoins(value: Int): String =
    String.format(java.util.Locale("pt", "BR"), "%,d", value)

@Composable
fun ShopModal(
    inventory: PlayerInventory,
    items: List<ShopItem>,
    onBuyItem: (ShopItem) -> Unit,
    onEquipItem: (ShopItem) -> Unit,
    onAddCoins: (Int) -> Unit,
    onClose: () -> Unit,
    operationMessage: String = ""
) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    val categories = listOf(
        "SKINS DE BLOCOS",
        "TABULEIROS",
        "AVATARES",
        "MOLDURAS",
        "BANNERS",
        "FUNDOS",
        "EFEITOS",
        "MÚSICAS",
        "ANIMAÇÕES"
    )
    val categoryKeys = listOf(
        "blocos",
        "tabuleiros",
        "avatares",
        "molduras",
        "banners",
        "fundos",
        "efeitos",
        "musicas",
        "animacoes"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "NeonTransition")
    val neonGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neonGlow"
    )

    val categoryKey = categoryKeys[selectedTabIndex]
    val filteredItems = items.filter { it.itemCategory == categoryKey }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ShBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize().appSafeDrawingPadding()) {
                ShopHeader(onBack = onClose)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ShOutlineVariant.copy(alpha = 0.5f))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Saldo + recarregar
                    BalanceCard(coins = inventory.coins)

                    if (operationMessage.isNotBlank()) {
                        Text(text = operationMessage, color = Color(0xFF00E5FF), fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Categorias (rolagem horizontal)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ShSurfaceLow)
                            .horizontalScroll(rememberScrollState())
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEachIndexed { index, title ->
                            val isSelected = selectedTabIndex == index
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ShCyan else Color.Transparent)
                                    .clickable { selectedTabIndex = index }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = categoryIcon(categoryKeys[index]),
                                    contentDescription = null,
                                    tint = if (isSelected) ShOnCyan else ShOnSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = title,
                                    color = if (isSelected) ShOnCyan else ShOnSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum item nesta categoria.",
                                color = ShOnSurfaceVariant,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 64.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                ShopItemCard(
                                    item = item,
                                    isBought = inventory.boughtItems.contains(item.id),
                                    isEquipped = inventory.equippedItems[item.itemCategory] == item.id,
                                    onBuy = { onBuyItem(item) },
                                    onEquip = { onEquipItem(item) },
                                    neonGlowAlpha = neonGlowAlpha
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ShSurfaceHigh)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = ShOnSurface,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ShCyan.copy(alpha = 0.12f))
                .border(1.dp, ShCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = "Loja",
                tint = ShCyan,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "LOJA",
            color = ShOnSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun CoinIcon(size: androidx.compose.ui.unit.Dp) {
    BlockoinIcon(size)
}

@Composable
private fun BalanceCard(coins: Int) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ShSurfaceLow)
            .border(1.dp, ShOutlineVariant.copy(alpha = 0.4f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinIcon(32.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "SALDO ATUAL",
                    color = ShOnSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = formatCoins(coins),
                    color = ShOnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }


    }
}

@Composable
internal fun BlockSkinPreview(skinId: String) {
    val orange = Cell(Color(0xFFFFB300), Color(0xFFFFE082), Color(0xFFB26A00))
    val green = Cell(Color(0xFF3DDC97), Color(0xFF9CF0C9), Color(0xFF1E8F61))
    val cyan = Cell(Color(0xFF00E5FF), Color(0xFF84FFFF), Color(0xFF0097A7))

    Canvas(modifier = Modifier.size(width = 72.dp, height = 48.dp)) {
        val s = size.width / 3f
        PixelBlockDrawer.drawPixelBlock(this, Offset(s, 0f), s, orange, 1f, skinId)
        PixelBlockDrawer.drawPixelBlock(this, Offset(0f, s), s, green, 1f, skinId)
        PixelBlockDrawer.drawPixelBlock(this, Offset(s, s), s, cyan, 1f, skinId)
        PixelBlockDrawer.drawPixelBlock(this, Offset(2f * s, s), s, green, 1f, skinId)
    }
}

@Composable
private fun ShopPreview(item: ShopItem, rarity: ShRarity) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    listOf(rarity.color.copy(alpha = 0.12f), ShSurfaceLowest)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        CosmeticThumbnail(item, Modifier.size(width = 120.dp, height = 72.dp))

        // Etiqueta de raridade
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(rarity.color.copy(alpha = 0.15f))
                .border(1.dp, rarity.color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text(
                text = rarity.label,
                color = rarity.color,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ShopActionButton(
    text: String,
    color: Color,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.6f), shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun ShopItemCard(
    item: ShopItem,
    isBought: Boolean,
    isEquipped: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    neonGlowAlpha: Float
) {
    val rarity = rarityOf(item.price)
    val shape = RoundedCornerShape(12.dp)
    val owned = isBought || isEquipped

    val borderColor = when {
        isEquipped -> ShLime.copy(alpha = neonGlowAlpha)
        isBought -> ShPurple.copy(alpha = 0.6f)
        rarity == ShRarity.EPICA || rarity == ShRarity.LENDARIA -> rarity.color.copy(alpha = 0.45f)
        else -> ShOutlineVariant.copy(alpha = 0.5f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ShSurface)
            .border(if (isEquipped) 1.5.dp else 1.dp, borderColor, shape)
            .padding(10.dp)
    ) {
        ShopPreview(item = item, rarity = rarity)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.name,
            color = ShOnSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (item.description.isNotBlank()) item.description else categoryLabel(item.itemCategory),
            color = ShOnSurfaceVariant,
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif,
            minLines = 2,
            maxLines = 2,
            lineHeight = 16.sp,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ShOutlineVariant.copy(alpha = 0.4f))
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preço / status
            when {
                owned -> Text(
                    text = "ADQUIRIDO",
                    color = ShCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                item.price <= 0 -> Text(
                    text = "GRÁTIS",
                    color = ShLime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(24.dp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = formatCoins(item.price),
                        color = ShOnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }

            // Ação
            when {
                isEquipped -> ShopActionButton(
                    text = "EQUIPADO",
                    color = ShLime,
                    icon = Icons.Default.Check
                )
                isBought -> ShopActionButton(
                    text = "EQUIPAR",
                    color = ShPurpleSoft,
                    onClick = onEquip
                )
                else -> ShopActionButton(
                    text = "COMPRAR",
                    color = rarity.color,
                    onClick = onBuy
                )
            }
        }
    }
}
