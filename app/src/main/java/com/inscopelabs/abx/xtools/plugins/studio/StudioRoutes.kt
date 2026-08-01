package com.inscopelabs.abx.xtools.plugins.studio

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

/**
 * Centralizes the route constants the Studio's fragments navigate by.
 * Keeping them in one file means renaming a screen doesn't break
 * cross-references in the codebase.
 */
object StudioRoutes {
    const val STUDIO = "studio"
    const val EXPLORER = "explorer"
    const val MANIFEST_EDITOR = "manifest"
    const val PERMISSION_EDITOR = "permissions"
    const val CODE_EDITOR = "code/{path}"
    const val ASSET_MANAGER = "assets"
    const val PREVIEW = "preview"
    const val BUILD = "build"

    fun codeEditor(path: String): String = "code/${java.net.URLEncoder.encode(path, "UTF-8")}"
}

/**
 * Tiny helper that resolves a [NavController] from any [Fragment] without
 * pulling the navigation-runtime artifact into the SDK. The host wires
 * the real graph; we just look up the controller.
 */
fun Fragment.studioNavController(): NavController {
    val host = requireActivity().supportFragmentManager
        .findFragmentById(android.R.id.content) as? NavHostFragment
        ?: error("no NavHostFragment attached to the activity")
    return host.navController
}
