package xyz.tyiu.satsprice.ui

import androidx.compose.ui.unit.Dp

/**
 * Horizontal inset from the screen edge to the section cards. Differs per platform because
 * touch-target density and window scale differ enough that a shared constant looks wrong on
 * at least one of them (e.g. Android phones want a tighter edge than a desktop window does).
 */
expect val screenHorizontalPadding: Dp
