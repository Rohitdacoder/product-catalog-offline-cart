package com.product_catalog_offline_cart.ui.products

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.product_catalog_offline_cart.R
import com.product_catalog_offline_cart.domain.model.Product
import com.product_catalog_offline_cart.ui.theme.ProductcatalogofflinecartTheme
import java.util.Locale

/** Stateful entry point: connects the ViewModel to the stateless [ProductListScreen]. */
@Composable
fun ProductListRoute(
    modifier: Modifier = Modifier,
    viewModel: ProductListViewModel = viewModel(factory = ProductListViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    ProductListScreen(
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun ProductListScreen(
    uiState: ProductListUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ProductSearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                ProductListUiState.Loading -> LoadingContent()
                ProductListUiState.Empty -> EmptyContent()
                is ProductListUiState.Error -> ErrorContent(
                    message = stringResource(uiState.messageRes),
                    onRetry = onRetry,
                )
                is ProductListUiState.Success -> ProductList(products = uiState.products)
            }
        }
    }
}

@Composable
private fun ProductSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text(stringResource(R.string.product_search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.product_search_clear))
                }
            }
        },
        singleLine = true,
    )
}

@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun LoadingContent() = CenteredContent {
    CircularProgressIndicator()
}

@Composable
private fun EmptyContent() = CenteredContent {
    Text(
        text = stringResource(R.string.product_list_empty),
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) = CenteredContent {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
private fun ProductList(products: List<Product>) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(products, key = { it.id }) { product ->
            ProductItem(product = product)
        }
    }
}

@Composable
private fun ProductItem(product: Product, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = product.thumbnailUrl,
                contentDescription = product.title,
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatPrice(product.price),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                ProductRating(rating = product.rating)
            }
        }
    }
}

@Composable
private fun ProductRating(rating: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = stringResource(R.string.product_rating, rating),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

// DummyJSON prices are in USD.
private fun formatPrice(price: Double): String = String.format(Locale.US, "$%.2f", price)

private val previewProducts = listOf(
    Product(
        id = 1, title = "Essence Mascara Lash Princess", description = "", category = "beauty",
        price = 9.99, discountPercentage = 10.48, rating = 2.56, stock = 99, brand = "Essence",
        thumbnailUrl = "", imageUrls = emptyList(),
    ),
    Product(
        id = 2, title = "Eyeshadow Palette with Mirror", description = "", category = "beauty",
        price = 19.99, discountPercentage = 18.19, rating = 2.86, stock = 34, brand = null,
        thumbnailUrl = "", imageUrls = emptyList(),
    ),
)

@Preview(showBackground = true)
@Composable
private fun ProductListSuccessPreview() {
    ProductcatalogofflinecartTheme {
        ProductListScreen(
            uiState = ProductListUiState.Success(previewProducts),
            searchQuery = "",
            onSearchQueryChange = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductListErrorPreview() {
    ProductcatalogofflinecartTheme {
        ProductListScreen(
            uiState = ProductListUiState.Error(R.string.error_network),
            searchQuery = "phone",
            onSearchQueryChange = {},
            onRetry = {},
        )
    }
}
