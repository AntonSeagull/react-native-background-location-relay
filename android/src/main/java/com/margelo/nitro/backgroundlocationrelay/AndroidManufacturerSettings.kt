package com.margelo.nitro.backgroundlocationrelay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Locale

object AndroidManufacturerSettings {
  fun getManufacturerLabel(): String? {
    val manufacturer = Build.MANUFACTURER.trim()
    return manufacturer.takeIf { it.isNotEmpty() }
  }

  fun isSettingsAvailable(context: Context): Boolean {
    return resolveIntent(context, getCandidateIntents(context)) != null
  }

  fun openSettings(context: Context): Boolean {
    val intent = resolveIntent(context, getCandidateIntents(context)) ?: return false
    return ActivityIntents.start(context, intent)
  }

  private fun resolveIntent(context: Context, intents: List<Intent>): Intent? {
    val packageManager = context.packageManager
    for (intent in intents) {
      if (intent.resolveActivity(packageManager) != null) {
        return intent
      }
    }
    return null
  }

  private fun getCandidateIntents(context: Context): List<Intent> {
    val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)
    val brand = Build.BRAND.lowercase(Locale.US)
    val packageName = context.packageName

    val manufacturerIntents =
      when {
        manufacturer in XIAOMI_MANUFACTURERS || brand in XIAOMI_MANUFACTURERS ->
          xiaomiIntents(packageName)
        manufacturer in HUAWEI_MANUFACTURERS || brand in HUAWEI_MANUFACTURERS ->
          huaweiIntents()
        manufacturer == "samsung" -> samsungIntents()
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

    return manufacturerIntents + genericFallbackIntents(context)
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

  private fun samsungIntents(): List<Intent> {
    return listOf(
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

  private fun genericFallbackIntents(context: Context): List<Intent> {
    return listOf(
      Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = android.net.Uri.fromParts("package", context.packageName, null)
      },
    )
  }

  private val XIAOMI_MANUFACTURERS = setOf("xiaomi", "redmi", "poco")
  private val HUAWEI_MANUFACTURERS = setOf("huawei", "honor")
  private val OPPO_MANUFACTURERS = setOf("oppo", "realme")
  private val VIVO_MANUFACTURERS = setOf("vivo", "iqoo")
  private val LETV_MANUFACTURERS = setOf("letv", "leeco")
}
