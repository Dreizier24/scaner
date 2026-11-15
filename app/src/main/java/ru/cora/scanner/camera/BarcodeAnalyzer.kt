package ru.cora.scanner.camera

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcode: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_ITF,
            )
            .build(),
    )

    @Volatile
    private var lastValue: String? = null

    @Volatile
    private var lastTimestampMs: Long = 0L

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val raw = barcodes.firstOrNull()?.rawValue?.trim().orEmpty()
                if (raw.isNotEmpty() && shouldEmit(raw)) {
                    onBarcode(raw)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun shouldEmit(value: String): Boolean {
        val now = System.currentTimeMillis()
        val duplicate = value == lastValue && now - lastTimestampMs < 2_500L
        if (!duplicate) {
            lastValue = value
            lastTimestampMs = now
            return true
        }
        return false
    }
}
