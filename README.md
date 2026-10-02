# System App Demo

Small demo app to showcase system app functionality and development steps.

Not a very useful app by itself. The focus rather is to show different types of APIs and how they can be used inside system apps.

Check the git history to see the individual changes needed to enable the individual APIs / permissions.

Created with the help of Claude Code (Opus 5 / Sonnet 5).

## "Plain" Android APIs

Methods such as `WifiManager.getScanResults()` just need install / runtime permissions to function.

When installed as a system app, permissions exceptions can be used to pre-approve the runtime permission.

This is done by installing the file `etc/default-permissions/default-permissions-wifimanager.xml` in the code base to `/system/etc/default-permissions/` on the target.

[Documentation](https://source.android.com/docs/core/permissions/runtime_perms#creating-exceptions)

## @SystemAPI

The app calls `WifiManager.isPortableHotspotSupported()`, which is a method that is annotated as `@SystemApi` but requires no additional permissions.

Gradle will no longer be able to build this app once such an API is added, since the SDK it builds against contains a stripped down version of the Android APIs in its `android.jar`.

At this point, the app should be built using Soong and installed as a system app.

Alternatively, you can use a hack to keep using Gradle and Android Studio. 
Your AOSP environment might provide a version of the `android.jar` that includes the system APIs. 
If you copy this into your SDK folder, Gradle should be able to build the app again.

```bash
mv ~/Android/Sdk/platforms/android-36/android.jar ~/Android/Sdk/platforms/android-36/android.jar.bak
cp $AOSP/prebuilts/sdk/36/system/android.jar ~/Android/Sdk/platforms/android-36/android.jar
```

Check the [Metalava tool](https://android.googlesource.com/platform/tools/metalava) for more info.

## Privileged APIs

`WifiManager.getPrivilegedConfiguredNetworks()` requires the `READ_WIFI_CREDENTIAL` permission which has a protection level of `signature|privileged` meaning that the app either has to be signed with the platform key or installed as a privileged system app.

Privileged permissions must also be explicitly whitelisted for individual packages. This is usually done in a device-/product-wide file.

A sample file is included in `etc/permissions/privapp-permissions-rpi5.xml`.

The file can for example be installed on the target with the following Soong module:
```
prebuilt_etc {
    name: "device_brcm_rpi5_privapp_permissions_rpi5",
    srcs: [
        "privapp-permissions-rpi5.xml",
    ],
    relative_install_path: "permissions",
}
```

IMPORTANT: Without this allow list, the target will fail to boot!

This behaviour can be changed for debug purposes by setting the following property:
```
PRODUCT_SYSTEM_PROPERTIES += ro.control_privapp_permissions=log
```

[Documentation](https://source.android.com/docs/core/permissions/perms-allowlist)

[Cuttlefish example](https://cs.android.com/android/platform/superproject/+/android-16.0.0_r4:device/google/cuttlefish/shared/permissions/privapp-permissions-cuttlefish.xml)
