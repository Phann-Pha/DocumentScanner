package com.pha.document.scanner.common.documentscanner.model

data class ErrorScannerModel(var message: ErrorMessage? = null, var throwable: Throwable? = null)
{
    enum class ErrorMessage
    {
        TAKE_IMAGE_FROM_GALLERY_ERROR,
        PHOTO_CAPTURE_FAILED,
        CAMERA_USE_CASE_BINDING_FAILED,
        DETECT_LARGEST_QUADRILATERAL_FAILED,
        INVALID_IMAGE,
        CAMERA_PERMISSION_REFUSED_WITHOUT_NEVER_ASK_AGAIN,
        CAMERA_PERMISSION_REFUSED_GO_TO_SETTINGS,
        STORAGE_PERMISSION_REFUSED_WITHOUT_NEVER_ASK_AGAIN,
        STORAGE_PERMISSION_REFUSED_GO_TO_SETTINGS,
        CROPPING_FAILED,
        ERROR_RECOGNIZE_ID_CARD,
        INVALID_NATIONALITY_CARD,
        MAYBE_NATIONALITY_CARD,
    }
}