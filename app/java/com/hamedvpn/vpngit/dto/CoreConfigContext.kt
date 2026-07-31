package com.hamedvpn.vpngit.dto

import android.content.Context
import com.hamedvpn.vpngit.dto.entities.ProfileItem
import com.hamedvpn.vpngit.enums.CoreResolvedType

data class CoreConfigContext(
    val context: Context,
    val guid: String,
    val isCustom: Boolean = false,
    val resolvedOutbounds: List<ResolvedOutbound> = emptyList(),
) {
    data class ResolvedOutbound(
        val tag: String,
        val profile: ProfileItem,
        val resolvedProfiles: List<ProfileItem>,
        val resolvedType: CoreResolvedType,
    )
}
