

patchClass('some.class.Name')
        .replaceMethodCall("android.telephony.TelephonyManager#getDeviceId")
        .with(sprintf('{ $_ = "%s00"; }', "123456"))
        .replaceMethodCall("android.net.wifi.WifiManager#getConnectionInfo")
        .with(' { $_ = null; } ')
        .saveTo(args[0])
