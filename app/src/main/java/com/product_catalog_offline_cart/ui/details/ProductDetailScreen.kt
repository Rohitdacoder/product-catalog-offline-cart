package com.product_catalog_offline_cart.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.product_catalog_offline_cart.R
import com.product_catalog_offline_cart.domain.model.Product
import com.product_catalog_offline_cart.ui.common.formatPrice
import com.product_catalog_offline_cart.ui.theme.ProductcatalogofflinecartTheme
import kotlinx.coroutines.flow.collectLatest

/** Stateful entry point: connects the ViewModel to the stateless [ProductDetailScreen]. */
@Composable
fun ProductDetailRoute(
    viewModel: ProductDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        // collectLatest: a new tap replaces the snackbar that's showing instead of queueing behind it.
        viewModel.cartMessages.collectLatest { messageRes ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(context.getString(messageRes))
        }
    }

    ProductDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRetry = viewModel::retry,
        onAddToCart = viewModel::addToCart,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    uiState: ProductDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.product_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (uiState is ProductDetailUiState.Success) {
                AddToCartBar(product = uiState.product, onAddToCart = onAddToCart)
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (uiState) {
                ProductDetailUiState.Loading -> LoadingContent()
                is ProductDetailUiState.Error -> ErrorContent(
                    message = stringResource(uiState.messageRes),
                    onRetry = onRetry,
                )
                is ProductDetailUiState.Success -> ProductDetailContent(product = uiState.product)
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun ProductDetailContent(product: Product) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        ProductImage(
            imageUrl = product.imageUrls.firstOrNull() ?: product.thumbnailUrl,
            contentDescription = product.title,
        )
        Spacer(Modifier.height(16.dp))
        Text(text = product.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            text = formatPrice(product.price),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        RatingRow(rating = product.rating)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        DetailRow(label = stringResource(R.string.product_category), value = product.category.toDisplayCategory())
        // Not every product has a brand (e.g. groceries), so the row is left out rather than showing a blank.
        product.brand?.let { DetailRow(label = stringResource(R.string.product_brand), value = it) }
        DetailRow(
            label = stringResource(R.string.product_stock),
            value = if (product.stock > 0) {
                stringResource(R.string.product_in_stock, product.stock)
            } else {
                stringResource(R.string.product_out_of_stock)
            },
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        Text(text = product.description, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ProductImage(imageUrl: String, contentDescription: String) {
    SubcomposeAsyncImage(
        model = imageUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        loading = {
            Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        },
        error = {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.product_image_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
private fun RatingRow(rating: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.product_rating, rating),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddToCartBar(product: Product, onAddToCart: (Product) -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Button(
            onClick = { onAddToCart(product) },
            enabled = product.stock > 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.add_to_cart))
        }
    }
}

/** DummyJSON categories are slugs, e.g. "mobile-accessories" → "Mobile accessories". */
private fun String.toDisplayCategory(): String =
    replace('-', ' ').replaceFirstChar { it.titlecase() }

private val previewProduct = Product(
    id = 1,
    title = "Essence Mascara Lash Princess with an Extra Long Name to Check Wrapping",
    description = "The Essence Mascara Lash Princess is a popular mascara known for its volumizing " +
        "and lengthening effects. Achieve dramatic lashes with this long-lasting and cruelty-free formula.",
    category = "beauty",
    price = 9.99,
    discountPercentage = 10.48,
    rating = 2.56,
    stock = 99,
    brand = null,
    thumbnailUrl = "",
    imageUrls = emptyList(),
)

@Preview(showBackground = true)
@Composable
private fun ProductDetailSuccessPreview() {
    ProductcatalogofflinecartTheme {
        ProductDetailScreen(
            uiState = ProductDetailUiState.Success(previewProduct),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onRetry = {},
            onAddToCart = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductDetailErrorPreview() {
    ProductcatalogofflinecartTheme {
        ProductDetailScreen(
            uiState = ProductDetailUiState.Error(R.string.error_network),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onRetry = {},
            onAddToCart = {},
        )
    }
}
