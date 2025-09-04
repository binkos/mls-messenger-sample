package com.messenger.sample.desktop.ui.models

import uniffi.mls_rs_uniffi.Client

class DesktopUser(
    val userId: String,
    val userName: String,
    val client: Client
)