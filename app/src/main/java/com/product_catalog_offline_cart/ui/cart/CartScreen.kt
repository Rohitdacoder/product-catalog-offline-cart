package com.product_catalog_offline_cart.ui.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.product_catalog_offline_cart.R
import com.product_catalog_offline_cart.domain.model.CartItem
import com.product_catalog_offline_cart.ui.common.formatPrice
import com.product_catalog_offline_cart.ui.theme.ProductcatalogofflinecartTheme

/** Stateful entry point: connects the ViewModel to the stateless [CartScreen]. */
@Composable
fun CartRoute(
    onBack: () -> Unit,
    onContinueShopping: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = viewModel(factory = CartViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CartScreen(
        uiState = uiState,
        onBack = onBack,
        onContinueShopping = onContinueShopping,
        onIncrease = viewModel::increaseQuantity,
        onDecrease = viewModel::decreaseQuantity,
        onRemove = viewModel::removeFromCart,
        onClearCart = viewModel::clearCart,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    uiState: CartUiState,
    onBack: () -> Unit,
    onContinueShopping: () -> Unit,
    onIncrease: (productId: Int) -> Unit,
    onDecrease: (productId: Int) -> Unit,
    onRemove: (productId: Int) -> Unit,
    onClearCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Dialog visibility is purely UI state, so it lives here rather than in the ViewModel.
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cart_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                actions = {
                    if (uiState is CartUiState.Content) {
                        TextButton(onClick = { showClearDialog = true }) {
                            Text(stringResource(R.string.cart_clear))
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (uiState is CartUiState.Content) {
                CartSummaryBar(totalItems = uiState.totalItems, totalPrice = uiState.totalPrice)
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (uiState) {
                CartUiState.Loading -> LoadingContent()
                CartUiState.Empty -> EmptyCartContent(onContinueShopping = onContinueShopping)
                is CartUiState.Content -> CartItemList(
                    items = uiState.items,
                    onIncrease = onIncrease,
                    onDecrease = onDecrease,
                    onRemove = onRemove,
                )
            }
        }
    }

    if (showClearDialog) {
        ClearCartDialog(
            onConfirm = {
                showClearDialog = false
                onClearCart()
            },
            onDismiss = { showClearDialog = false },
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyCartContent(onContinueShopping: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.cart_empty),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onContinueShopping) {
            Text(stringResource(R.string.cart_continue_shopping))
        }
    }
}

@Composable
private fun CartItemList(
    items: List<CartItem>,
    onIncrease: (productId: Int) -> Unit,
    onDecrease: (productId: Int) -> Unit,
    onRemove: (productId: Int) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.productId }) { item ->
            CartItemRow(
                item = item,
                onIncrease = { onIncrease(item.productId) },
                onDecrease = { onDecrease(item.productId) },
                onRemove = { onRemove(item.productId) },
            )
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Stored URL from Room; offline, an uncached image falls back to the placeholder color.
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.cart_unit_price, formatPrice(item.price)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuantityStepper(
                        title = item.title,
                        quantity = item.quantity,
                        onIncrease = onIncrease,
                        onDecrease = onDecrease,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = formatPrice(item.lineTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.cart_remove_item, item.title),
                )
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    title: String,
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onDecrease, modifier = Modifier.size(36.dp)) {
            Icon(RemoveIcon, contentDescription = stringResource(R.string.cart_decrease_quantity, title))
        }
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 40.dp),
        )
        FilledTonalIconButton(onClick = onIncrease, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cart_increase_quantity, title))
        }
    }
}

@Composable
private fun CartSummaryBar(totalItems: Int, totalPrice: Double) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cart_total_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = pluralStringResource(R.plurals.cart_total_items, totalItems, totalItems),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = formatPrice(totalPrice),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ClearCartDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cart_clear_confirm_title)) },
        text = { Text(stringResource(R.string.cart_clear_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.cart_clear_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Minus icon (same path as Material's "Remove"), which isn't part of material-icons-core. */
private val RemoveIcon: ImageVector = materialIcon(name = "Filled.Remove") {
    materialPath {
        moveTo(19.0f, 13.0f)
        horizontalLineTo(5.0f)
        verticalLineToRelative(-2.0f)
        horizontalLineToRelative(14.0f)
        verticalLineToRelative(2.0f)
        close()
    }
}

private val previewItems = listOf(
    CartItem(productId = 1, title = "Essence Mascara Lash Princess", price = 9.99, thumbnailUrl = "", quantity = 2),
    CartItem(productId = 2, title = "iPhone 9", price = 549.0, thumbnailUrl = "", quantity = 1),
)

@Preview(showBackground = true)
@Composable
private fun CartContentPreview() {
    ProductcatalogofflinecartTheme {
        CartScreen(
            uiState = CartUiState.Content(previewItems, totalItems = 3, totalPrice = 568.98),
            onBack = {},
            onContinueShopping = {},
            onIncrease = {},
            onDecrease = {},
            onRemove = {},
            onClearCart = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CartEmptyPreview() {
    ProductcatalogofflinecartTheme {
        CartScreen(
            uiState = CartUiState.Empty,
            onBack = {},
            onContinueShopping = {},
            onIncrease = {},
            onDecrease = {},
            onRemove = {},
            onClearCart = {},
        )
    }
}
