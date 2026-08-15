package app.pwhs.universalinstaller.util

import android.content.Context
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.IBinder
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import timber.log.Timber

object HiddenApiHacks {

    private fun addExemptions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/content/pm/IPackageManager",
                "Landroid/content/pm/IPackageInstaller",
                "Landroid/content/pm/IPackageInstallerSession",
                "Landroid/content/pm/PackageInstaller",
                "Landroid/os/UserHandle",
            )
        }
    }

    private fun remotePackageInstaller(): IPackageInstaller {
        val packageBinder = SystemServiceHelper.getSystemService("package")
        val iPackageManager = IPackageManager.Stub.asInterface(ShizukuBinderWrapper(packageBinder))
        return IPackageInstaller.Stub.asInterface(
            ShizukuBinderWrapper(iPackageManager.packageInstaller.asBinder())
        )
    }

    fun openWrappedSession(sessionId: Int): PackageInstaller.Session? {
        addExemptions()
        return try {
            val remoteSession = IPackageInstallerSession.Stub.asInterface(
                ShizukuBinderWrapper(remotePackageInstaller().openSession(sessionId).asBinder())
            )
            PackageInstaller.Session::class.java
                .getDeclaredConstructor(IPackageInstallerSession::class.java)
                .apply { isAccessible = true }
                .newInstance(remoteSession)
        } catch (t: Throwable) {
            Timber.e(t, "openWrappedSession failed (sessionId=$sessionId)")
            null
        }
    }

    fun createPackageInstallerForUser(context: Context, userId: Int, overrideInstallerPackageName: String? = null): PackageInstaller? {
        addExemptions()
        return try {
            val iPackageInstaller = remotePackageInstaller()
            val installerPackageName = overrideInstallerPackageName ?: if (rikka.shizuku.Shizuku.getUid() == 0) {
                context.packageName
            } else {
                "com.android.shell"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val constructor = PackageInstaller::class.java.getDeclaredConstructor(
                    IPackageInstaller::class.java, String::class.java, String::class.java, Int::class.java
                )
                constructor.isAccessible = true
                constructor.newInstance(iPackageInstaller, installerPackageName, context.attributionTag, userId)
            } else {
                val constructor = PackageInstaller::class.java.getDeclaredConstructor(
                    IPackageInstaller::class.java, String::class.java, Int::class.java
                )
                constructor.isAccessible = true
                constructor.newInstance(iPackageInstaller, installerPackageName, userId)
            }
        } catch (t: Throwable) {
            Timber.e(t, "createPackageInstallerForUser failed (userId=$userId)")
            null
        }
    }

    /**
     * SessionParams com as flags privilegiadas que uma sessão dona do shell precisa pra
     * substituir um pacote já existente — incluindo apps de sistema. Sem isso, o commit
     * falha com INSTALL_FAILED_ALREADY_EXISTS assim que o pacote alvo já existe.
     * Baseado na abordagem do vvb2060/PackageInstaller (createSessionParams).
     */
    fun createPrivilegedSessionParams(allowDowngrade: Boolean = false): PackageInstaller.SessionParams {
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        try {
            val field = PackageInstaller.SessionParams::class.java.getDeclaredField("installFlags")
            field.isAccessible = true
            var flags = field.getInt(params)

            val INSTALL_REPLACE_EXISTING = 0x00000002
            val INSTALL_ALLOW_TEST = 0x00000004
            val INSTALL_REQUEST_DOWNGRADE = 0x00000080
            val INSTALL_FULL_APP = 0x00004000
            val INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK = 0x00400000
            val INSTALL_REQUEST_UPDATE_OWNERSHIP = 0x00800000

            flags = flags or INSTALL_REPLACE_EXISTING or INSTALL_ALLOW_TEST or INSTALL_FULL_APP
            if (allowDowngrade) flags = flags or INSTALL_REQUEST_DOWNGRADE
            if (Build.VERSION.SDK_INT >= 34) {
                flags = flags or INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK or INSTALL_REQUEST_UPDATE_OWNERSHIP
            }
            field.setInt(params, flags)
        } catch (t: Throwable) {
            Timber.e(t, "Failed to set privileged installFlags — falling back to default session params")
        }
        return params
    }

    fun currentUserId(): Int = android.os.Process.myUid() / 100000

    fun setApplicationEnabledSetting(packageName: String, newState: Int, flags: Int) {
        try {
            val packageBinder = SystemServiceHelper.getSystemService("package")
            val iPackageManager = IPackageManager.Stub.asInterface(ShizukuBinderWrapper(packageBinder))
            try {
                val method = iPackageManager.javaClass.getMethod(
                    "setApplicationEnabledSetting",
                    String::class.java, Int::class.java, Int::class.java, Int::class.java, String::class.java
                )
                method.invoke(iPackageManager, packageName, newState, flags, 0, "com.android.shell")
                return
            } catch (e: NoSuchMethodException) {
                val method = iPackageManager.javaClass.getMethod(
                    "setApplicationEnabledSetting",
                    String::class.java, Int::class.java, Int::class.java, Int::class.java
                )
                method.invoke(iPackageManager, packageName, newState, flags, 0)
            }
        } catch (e: Exception) {
            try {
                val newProcessMethod = rikka.shizuku.Shizuku::class.java.getMethod(
                    "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
                )
                val process = newProcessMethod.invoke(
                    null, arrayOf("pm", if (newState <= 1) "enable" else "disable-user", packageName), null, null
                ) as Process
                process.waitFor()
            } catch (ex: Exception) { }
        }
    }
}
