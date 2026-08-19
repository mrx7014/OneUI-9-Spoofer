package com.HeheJuice.OneUINineSpoof

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import de.robv.android.xposed.XC_MethodHook
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

class HeheJuiceSpoof : IXposedHookLoadPackage {

    private val customXmlContent = """
        <?xml version="1.0" encoding="utf-8"?>
        <permissions>
         <feature name="com.samsung.android.oneui.version.10000" />
         <feature name="com.samsung.android.oneui.version.10100" />
         <feature name="com.samsung.android.oneui.version.10200" />
         <feature name="com.samsung.android.oneui.version.10500" />
         <feature name="com.samsung.android.oneui.version.20000" />
         <feature name="com.samsung.android.oneui.version.20100" />
         <feature name="com.samsung.android.oneui.version.20500" />
         <feature name="com.samsung.android.oneui.version.30000" />
         <feature name="com.samsung.android.oneui.version.30100" />
         <feature name="com.samsung.android.oneui.version.30101" />
         <feature name="com.samsung.android.oneui.version.40000" />
         <feature name="com.samsung.android.oneui.version.40100" />
         <feature name="com.samsung.android.oneui.version.40101" />
         <feature name="com.samsung.android.oneui.version.50000" />
         <feature name="com.samsung.android.oneui.version.50100" />
         <feature name="com.samsung.android.oneui.version.50101" />
         <feature name="com.samsung.android.oneui.version.60000" />
         <feature name="com.samsung.android.oneui.version.60100" /> 
         <feature name="com.samsung.android.oneui.version.60101" /> 
         <feature name="com.samsung.android.oneui.version.70000" />
         <feature name="com.samsung.android.oneui.version.80000" /> 
         <feature name="com.samsung.android.oneui.version.90000" /> 
         <feature name="com.samsung.android.oneui.version.90000" /> 
        </permissions>
    """.trimIndent()

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName == null) return

        val targetPath = "/system/etc/permissions/com.samsung.android.oneui.version.xml"
        val xmlBytes = customXmlContent.toByteArray(Charsets.UTF_8)

        // =========================================================
        // LAYER 1: SYSTEM PROPERTIES & INT OVERRIDES (All Apps)
        // =========================================================
        try {
            val systemPropertiesClass = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader)

            val propHookString = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                    if (key == "ro.build.version.oneui") param.result = "80000" 
                    if (key == "ro.build.version.sep") param.result = "170000"
                }
            }
            XposedHelpers.findAndHookMethod(systemPropertiesClass, "get", String::class.java, propHookString)
            XposedHelpers.findAndHookMethod(systemPropertiesClass, "get", String::class.java, String::class.java, propHookString)

            val propHookInt = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                    if (key == "ro.build.version.oneui") param.result = 80000
                    if (key == "ro.build.version.sep") param.result = 170000
                }
            }
            XposedHelpers.findAndHookMethod(systemPropertiesClass, "getInt", String::class.java, Int::class.javaPrimitiveType, propHookInt)
        } catch (t: Throwable) {}

        // 1B. Spoof PackageManager.hasSystemFeature
        try {
            val featureHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val featureName = param.args[0] as? String ?: return
                    if (featureName.startsWith("com.samsung.android.oneui.version")) {
                        param.result = true
                    }
                }
            }
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "hasSystemFeature", String::class.java, featureHook)
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "hasSystemFeature", String::class.java, Int::class.javaPrimitiveType, featureHook)
        } catch (t: Throwable) {}

        // 1C. Spoof Samsung's Hidden Static Build Variables
        try {
            val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
            XposedHelpers.setStaticIntField(buildVersionClass, "SEM_PLATFORM_INT", 170000)
            XposedHelpers.setStaticIntField(buildVersionClass, "SEM_INT", 170000)
        } catch (t: Throwable) {}

        // 1D. Spoof Samsung's Proprietary SemSystemProperties Wrapper
        try {
            val semSystemPropertiesClass = XposedHelpers.findClass("android.os.SemSystemProperties", lpparam.classLoader)
            
            val semPropHookString = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                    if (key == "ro.build.version.oneui") param.result = "80000"
                    if (key == "ro.build.version.sep") param.result = "170000"
                }
            }
            XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "get", String::class.java, semPropHookString)
            XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "get", String::class.java, String::class.java, semPropHookString)

            val semPropHookInt = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                    if (key == "ro.build.version.oneui") param.result = 80000
                    if (key == "ro.build.version.sep") param.result = 170000
                }
            }
            XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "getInt", String::class.java, Int::class.javaPrimitiveType, semPropHookInt)
        } catch (t: Throwable) {}

        // =========================================================
        // LAYER 2: UNIVERSAL VIRTUAL FILE SIMULATION (All Apps)
        // =========================================================
        
        // 2A. Mock existence globally
        try {
            XposedHelpers.findAndHookMethod(File::class.java, "exists", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val file = param.thisObject as File
                    if (file.absolutePath == targetPath) {
                        param.result = true
                    }
                }
            })
        } catch (t: Throwable) {}

        // 2B. Mock file length metrics
        try {
            XposedHelpers.findAndHookMethod(File::class.java, "length", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val file = param.thisObject as File
                    if (file.absolutePath == targetPath) {
                        param.result = xmlBytes.size.toLong()
                    }
                }
            })
        } catch (t: Throwable) {}

        // 2C. Intercept FileInputStream constructors safely via /dev/null redirect
        try {
            val fileStreamHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val arg = param.args[0]
                    val path = if (arg is File) arg.absolutePath else arg as? String
                    
                    if (path == targetPath) {
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "isOneUISpoofStream", true)
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "spoofStream", ByteArrayInputStream(xmlBytes))
                        if (arg is File) {
                            param.args[0] = File("/dev/null")
                        } else {
                            param.args[0] = "/dev/null"
                        }
                    }
                }
            }
            XposedHelpers.findAndHookConstructor(FileInputStream::class.java, File::class.java, fileStreamHook)
            XposedHelpers.findAndHookConstructor(FileInputStream::class.java, String::class.java, fileStreamHook)
        } catch (t: Throwable) {}

        // 2D. Handle byte-array parsing transactions directly
        try {
            XposedHelpers.findAndHookMethod(FileInputStream::class.java, "read", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (XposedHelpers.getAdditionalInstanceField(param.thisObject, "isOneUISpoofStream") == true) {
                        val bis = XposedHelpers.getAdditionalInstanceField(param.thisObject, "spoofStream") as ByteArrayInputStream
                        param.result = bis.read()
                    }
                }
            })

            XposedHelpers.findAndHookMethod(FileInputStream::class.java, "read", ByteArray::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (XposedHelpers.getAdditionalInstanceField(param.thisObject, "isOneUISpoofStream") == true) {
                        val bis = XposedHelpers.getAdditionalInstanceField(param.thisObject, "spoofStream") as ByteArrayInputStream
                        val b = param.args[0] as ByteArray
                        param.result = bis.read(b)
                    }
                }
            })

            XposedHelpers.findAndHookMethod(FileInputStream::class.java, "read", ByteArray::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (XposedHelpers.getAdditionalInstanceField(param.thisObject, "isOneUISpoofStream") == true) {
                        val bis = XposedHelpers.getAdditionalInstanceField(param.thisObject, "spoofStream") as ByteArrayInputStream
                        val b = param.args[0] as ByteArray
                        val off = param.args[1] as Int
                        val len = param.args[2] as Int
                        param.result = bis.read(b, off, len)
                    }
                }
            })

            XposedHelpers.findAndHookMethod(FileInputStream::class.java, "available", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (XposedHelpers.getAdditionalInstanceField(param.thisObject, "isOneUISpoofStream") == true) {
                        val bis = XposedHelpers.getAdditionalInstanceField(param.thisObject, "spoofStream") as ByteArrayInputStream
                        param.result = bis.available()
                    }
                }
            })
        } catch (t: Throwable) {}
    }
}
