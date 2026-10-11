package com.ah.filemanagerplus.core.utils

expect fun hasAskedShortcut(): Boolean
expect fun markShortcutAsked()
expect fun createDesktopShortcut(): Boolean
expect fun deleteDesktopShortcut(): Boolean
expect fun desktopShortcutExists(): Boolean