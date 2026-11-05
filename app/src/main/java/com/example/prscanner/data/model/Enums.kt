package com.example.prscanner.data.model

enum class FilterType(val displayName: String) {
    ORIGINAL("Original"),
    AUTO_ENHANCE("Auto Enhance"),
    MAGIC_COLOR("Magic Color"),
    DOCUMENT("Document"),
    BW("B & W"),
    GRAYSCALE("Grayscale"),
    LIGHTEN("Lighten"),
    DARKEN("Darken"),
    SHARPEN("Sharpen"),
    HIGH_CONTRAST("High Contrast")
}

enum class DocType(val displayName: String, val iconName: String) {
    GENERAL("Document", "description"),
    RECEIPT("Receipt", "receipt"),
    INVOICE("Invoice", "receipt_long"),
    ID_CARD("ID Card", "badge"),
    CONTRACT("Contract", "history_edu"),
    BOOK("Book", "auto_stories"),
    CERTIFICATE("Certificate", "workspace_premium"),
    NOTES("Notes", "edit_note")
}

enum class PageSizePreset(val displayName: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (210 x 297 mm)", 595, 842),
    US_LETTER("US Letter (8.5 x 11 in)", 612, 792),
    LEGAL("US Legal (8.5 x 14 in)", 612, 1008),
    A3("A3 (297 x 420 mm)", 842, 1191),
    ID_CARD("ID Card (85.6 x 54 mm)", 242, 153)
}

enum class SubscriptionTier(val title: String, val badge: String) {
    FREE("Free Plan", "FREE"),
    PRO("PR Scanner Pro", "PRO"),
    BUSINESS("PR Scanner Business", "BUSINESS")
}

enum class SyncStatus {
    LOCAL_ONLY,
    PENDING_SYNC,
    SYNCED,
    FAILED
}
