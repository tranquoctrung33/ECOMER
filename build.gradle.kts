plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Sử dụng alias thay vì id cứng
    alias(libs.plugins.google.services) apply false
}