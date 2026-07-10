package com.margelo.nitro.backgroundlocationrelay

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ContextThemeWrapper
import java.util.Locale

object AndroidAutostartSettings {
  /** Returns true if a vendor autostart screen exists and can be opened on this device. */
  fun isAvailable(context: Context): Boolean {
    val vendorIntents = getVendorIntents(context)
    if (vendorIntents.isEmpty()) {
      return false
    }
    val packageManager = context.packageManager
    return vendorIntents.any { it.resolveActivity(packageManager) != null }
  }

  fun openSettings(context: Context): Boolean {
    for (intent in getVendorIntents(context)) {
      if (ActivityIntents.start(context, Intent(intent))) {
        return true
      }
      RelayLogger.debug("Autostart intent did not open: $intent")
    }
    showUnavailableDialog(context)
    return false
  }

  private fun showUnavailableDialog(context: Context) {
    val appContext = context.applicationContext
    Handler(Looper.getMainLooper()).post {
      try {
        val themedContext =
          ContextThemeWrapper(appContext, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
        AlertDialog.Builder(themedContext)
          .setMessage(appContext.getString(R.string.autostart_unavailable_message))
          .setPositiveButton(android.R.string.ok, null)
          .show()
      } catch (error: Exception) {
        RelayLogger.error("Failed to show autostart unavailable dialog: ${error.message}")
      }
    }
  }

  private fun getVendorIntents(context: Context): List<Intent> {
    val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)
    val brand = Build.BRAND.lowercase(Locale.US)
    val packageName = context.packageName

    return when {
      manufacturer in XIAOMI_MANUFACTURERS || brand in XIAOMI_MANUFACTURERS ->
        xiaomiIntents(packageName)
      manufacturer in HUAWEI_MANUFACTURERS || brand in HUAWEI_MANUFACTURERS ->
        huaweiIntents()
      manufacturer == "samsung" -> samsungIntents(packageName)
      manufacturer in OPPO_MANUFACTURERS || brand in OPPO_MANUFACTURERS ->
        oppoIntents()
      manufacturer in VIVO_MANUFACTURERS || brand in VIVO_MANUFACTURERS ->
        vivoIntents()
      manufacturer == "oneplus" || brand == "oneplus" -> onePlusIntents()
      manufacturer == "meizu" -> meizuIntents()
      manufacturer == "asus" -> asusIntents()
      manufacturer == "nokia" -> nokiaIntents()
      manufacturer in LETV_MANUFACTURERS -> letvIntents()
      else -> emptyList()
    }
  }

  private fun componentIntent(packageName: String, className: String): Intent {
    return Intent().setComponent(ComponentName(packageName, className))
  }

  private fun xiaomiIntents(appPackageName: String): List<Intent> {
    return listOf(
      componentIntent(
        "com.miui.securitycenter",
        "com.miui.permcenter.autostart.AutoStartManagementActivity",
      ),
      Intent("miui.intent.action.OP_AUTO_START").setClassName(
        "com.miui.securitycenter",
        "com.miui.permcenter.autostart.AutoStartManagementActivity",
      ),
      Intent().apply {
        component =
          ComponentName(
            "com.miui.powerkeeper",
            "com.miui.powerkeeper.ui.HiddenAppsConfigActivity",
          )
        putExtra("package_name", appPackageName)
        putExtra("package_label", appPackageName)
      },
      componentIntent(
        "com.miui.securitycenter",
        "com.miui.permcenter.permissions.PermissionsEditorActivity",
      ),
    )
  }

  private fun huaweiIntents(): List<Intent> {
    return listOf(
      componentIntent(
        "com.huawei.systemmanager",
        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
      ),
      componentIntent(
        "com.huawei.systemmanager",
        "com.huawei.systemmanager.optimize.process.ProtectActivity",
      ),
      componentIntent(
        "com.huawei.systemmanager",
        "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
      ),
    )
  }

  private fun samsungIntents(appPackageName: String): List<Intent> {
    return listOf(
      Intent(SAMSUNG_ACTION_OPEN_CHECKABLE_LIST).apply {
        setPackage("com.samsung.android.lool")
        putExtra(SAMSUNG_ACTIVITY_TYPE_EXTRA, SAMSUNG_NEVER_SLEEPING_APPS)
      },
      componentIntent(
        "com.samsung.android.lool",
        "com.samsung.android.sm.battery.ui.BatteryActivity",
      ),
      componentIntent(
        "com.samsung.android.lool",
        "com.samsung.android.sm.ui.battery.BatteryActivity",
      ),
      componentIntent(
        "com.samsung.android.sm",
        "com.samsung.android.sm.ui.battery.BatteryActivity",
      ),
      componentIntent(
        "com.samsung.android.sm",
        "com.samsung.android.sm.battery.ui.usage.CheckableAppListActivity",
      ),
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", appPackageName, null)
      },
    )
  }

  private fun oppoIntents(): List<Intent> {
    return listOf(
      componentIntent(
        "com.coloros.safecenter",
        "com.coloros.safecenter.permission.startup.StartupAppListActivity",
      ),
      componentIntent(
        "com.oppo.safe",
        "com.oppo.safe.permission.startup.StartupAppListActivity",
      ),
      componentIntent(
        "com.coloros.safecenter",
        "com.coloros.safecenter.permission.floatwindow.FloatWindowListActivity",
      ),
    )
  }

  private fun vivoIntents(): List<Intent> {
    return listOf(
      componentIntent(
        "com.vivo.permissionmanager",
        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
      ),
      componentIntent(
        "com.iqoo.secure",
        "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
      ),
    )
  }

  private fun onePlusIntents(): List<Intent> {
    return listOf(
      componentIntent(
        "com.oneplus.security",
        "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",
      ),
      componentIntent(
        "com.oplus.battery",
        "com.oplus.battery.fuelgaue.PowerConsumptionOptimizationActivity",
      ),
    )
  }

  private fun meizuIntents(): List<Intent> {
    return listOf(
      componentIntent(
        "com.meizu.safe",
        "com.meizu.safe.permission.SmartBGActivity",
      ),
      componentIntent(
        "com.meizu.safe",
        "com.meizu.safe.security.HomeActivity",
      ),
    )
  }

  private fun asusIntents(): List<Intent> {
    return listOf(
      componentIntent(
        "com.asus.mobilemanager",
        "com.asus.mobilemanager.autostart.AutoStartActivity",
      ),
      componentIntent(
        "com.asus.mobilemanager",
        "com.asus.mobilemanager.entry.FunctionActivity",
      ),
    )
  }

  private fun nokiaIntents(): List<Intent> {
    return listOf(
      componentIntent(
        "com.evenwell.powersaving.g3",
        "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity",
      ),
    )
  }

  private fun letvIntents(): List<Intent> {
    return listOf(
      componentIntent(
        "com.letv.android.letvsafe",
        "com.letv.android.letvsafe.AutobootManageActivity",
      ),
    )
  }

  private const val SAMSUNG_ACTION_OPEN_CHECKABLE_LIST =
    "com.samsung.android.sm.ACTION_OPEN_CHECKABLE_LISTACTIVITY"
  private const val SAMSUNG_ACTIVITY_TYPE_EXTRA = "activity_type"
  private const val SAMSUNG_NEVER_SLEEPING_APPS = 2

  private val XIAOMI_MANUFACTURERS = setOf("xiaomi", "redmi", "poco")
  private val HUAWEI_MANUFACTURERS = setOf("huawei", "honor")
  private val OPPO_MANUFACTURERS = setOf("oppo", "realme")
  private val VIVO_MANUFACTURERS = setOf("vivo", "iqoo")
  private val LETV_MANUFACTURERS = setOf("letv", "leeco")
}
