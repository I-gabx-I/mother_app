package gt.marcos.joyeria.ui.product.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import gt.marcos.joyeria.R
import gt.marcos.joyeria.ui.format.format
import gt.marcos.joyeria.ui.theme.JoyeriaTheme

/**
 * Pantalla de alta rápida de pieza. Stateless y `@Preview`-able: recibe
 * estado y lambdas, nunca el ViewModel (CLAUDE.md sección 5). El wrapper
 * que sí conoce el ViewModel es `AddProductRoute`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    state: AddProductUiState,
    onTakePhotoClick: () -> Unit,
    onCostDigitsChange: (String) -> Unit,
    onSalePriceDigitsChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onSavedConfirmationDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var detailsExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.add_product_title)) }) },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PhotoField(photoPath = state.photoPath, onTakePhotoClick = onTakePhotoClick)

                MoneyDigitsField(
                    label = stringResource(R.string.add_product_cost_label),
                    digits = state.costDigits,
                    onDigitsChange = onCostDigitsChange,
                )
                MoneyDigitsField(
                    label = stringResource(R.string.add_product_sale_price_label),
                    digits = state.salePriceDigits,
                    onDigitsChange = onSalePriceDigitsChange,
                )

                state.suggestedPrice?.let { suggested ->
                    Text(
                        text = stringResource(R.string.add_product_suggested_price, suggested.format()),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                state.profit?.let { profit ->
                    Text(
                        text = stringResource(R.string.add_product_profit, profit.format()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                MoreDetailsSection(
                    expanded = detailsExpanded,
                    onExpandedChange = { detailsExpanded = it },
                    name = state.name,
                    onNameChange = onNameChange,
                    quantityText = state.quantityText,
                    onQuantityChange = onQuantityChange,
                    notes = state.notes,
                    onNotesChange = onNotesChange,
                )

                Button(
                    onClick = onSaveClick,
                    enabled = state.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(stringResource(R.string.add_product_save_button))
                }
            }

            state.savedUid?.let { uid ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .clickable { onSavedConfirmationDismissed() },
                ) {
                    Text(stringResource(R.string.add_product_saved_confirmation, uid))
                }
            }
        }
    }
}

@Composable
private fun PhotoField(photoPath: String?, onTakePhotoClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            if (photoPath != null) {
                AsyncImage(
                    model = photoPath,
                    contentDescription = stringResource(R.string.add_product_photo_description),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        TakePhotoButton(
            hasPhoto = photoPath != null,
            onClick = onTakePhotoClick,
        )
    }
}

@Composable
private fun TakePhotoButton(hasPhoto: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null)
            Text(
                stringResource(
                    if (hasPhoto) R.string.add_product_retake_photo else R.string.add_product_take_photo,
                ),
            )
        }
    }
}

@Composable
private fun MoreDetailsSection(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    quantityText: String,
    onQuantityChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
) {
    Column {
        TextButton(onClick = { onExpandedChange(!expanded) }, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.add_product_more_details))
                Icon(
                    imageVector = expandIcon(expanded),
                    contentDescription = null,
                )
            }
        }
        if (expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.add_product_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = onQuantityChange,
                    label = { Text(stringResource(R.string.add_product_quantity_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text(stringResource(R.string.add_product_notes_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun expandIcon(expanded: Boolean): ImageVector =
    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore

@Preview(showBackground = true)
@Composable
private fun AddProductScreenPreview() {
    JoyeriaTheme {
        AddProductScreen(
            state = AddProductUiState(
                costDigits = "4000",
                salePriceDigits = "8000",
                defaultMarkupBp = 10000,
                roundingStep = gt.marcos.joyeria.domain.model.Money(500),
            ),
            onTakePhotoClick = {},
            onCostDigitsChange = {},
            onSalePriceDigitsChange = {},
            onNameChange = {},
            onQuantityChange = {},
            onNotesChange = {},
            onSaveClick = {},
            onSavedConfirmationDismissed = {},
        )
    }
}
