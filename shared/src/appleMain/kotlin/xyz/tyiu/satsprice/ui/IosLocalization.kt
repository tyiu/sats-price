package xyz.tyiu.satsprice.ui

import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import dev.icerock.moko.resources.desc.desc

fun localizedString(resource: StringResource): String = resource.desc().localized()

fun localizedFormattedString(resource: StringResource, args: List<Any>): String =
    StringDesc.ResourceFormatted(resource, args).localized()
