package de.foellix.aql.jicer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import de.foellix.aql.helper.Helper;

@Tag("longLasting")
@Tag("systemIsSetup")
public class JicerTest {
	@Test
	public void test01() {
		test("-d debug -m slice -from \"Statement('$r1 = virtualinvoke $r3.<java.io.BufferedReader: java.lang.String readLine()>()')->Method('<com.adobe.flashplayer_.FlashW: java.lang.String readConfig(java.lang.String,android.content.Context)>')->Class('com.adobe.flashplayer_.FlashW')->App('example/backflash.apk')\" -to \"Statement('$r12 = virtualinvoke $r7.<java.net.URL: java.net.URLConnection openConnection()>()')->Method('<com.adobe.flashplayer_.FlashVirtual: java.lang.String doInBackground(java.lang.String[])>')->Class('com.adobe.flashplayer_.FlashVirtual')->App('example/backflash.apk')\"");
	}

	@Test
	public void test02() {
		test("-d debug -dg -m show -from \"Statement('$r5 = virtualinvoke $r4.<android.telephony.TelephonyManager: java.lang.String getSimSerialNumber()>()')->App('example/debug.apk')\"");
	}

	@Test
	public void test03() {
		test("-d debug -m show -si -from \"Statement('getSimSerialNumber')->App('example/debug.apk')\" -to \"Statement('startActivity')->App('example/debug.apk')\"");
	}

	@Test
	public void test04() {
		test("-d debug -m show -si -from \"Statement('startActivity')->App('example/debug.apk')\"");
	}

	@Test
	public void test05() {
		test("-d debug -m show -to \"Statement('$r5 = virtualinvoke $r4.<android.telephony.TelephonyManager: java.lang.String getSimSerialNumber()>()')->App('example/debug.apk')\"");
	}

	@Test
	public void test06() {
		test("-d debug -m show -si -to \"Statement('startActivity')->App('example/debug.apk')\"");
	}

	@Test
	public void test07() {
		test("-d debug -m slice -si -from \"Statement('getSimSerialNumber')->App('example/debug.apk')\" -to \"Statement('startActivity')->App('example/debug.apk')\"");
	}

	@Test
	public void test08() {
		test("-d debug -m slice -from \"Statement('$r5 = virtualinvoke $r4.<android.telephony.TelephonyManager: java.lang.String getSimSerialNumber()>()')->Method('<de.foellix.aql.slicer.slicertestapp.MainActivity: void onCreate(android.os.Bundle)>')->Class('de.foellix.aql.slicer.slicertestapp.MainActivity')->App('example/debug.apk')\" -to \"Statement('virtualinvoke $r0.<de.foellix.aql.slicer.slicertestapp.MainActivity: void startActivity(android.content.Intent)>($r1)')->Method('<de.foellix.aql.slicer.slicertestapp.MainActivity: void sink(android.content.Intent)>')->Class('de.foellix.aql.slicer.slicertestapp.MainActivity')->App('example/debug.apk') ! }) ?\"");
	}

	@Test
	public void test09() {
		test("-d debug -m slice -from \"Statement('$r5 = virtualinvoke $r4.<android.telephony.TelephonyManager: java.lang.String getSimSerialNumber()>()')->Method('<de.foellix.aql.slicer.slicertestapp.MainActivity: void onCreate(android.os.Bundle)>')->Class('de.foellix.aql.slicer.slicertestapp.MainActivity')->App('%APP_APK_FROM%')\" -to \"Statement('%STATEMENT_FROM%')->Method('<de.foellix.aql.slicer.slicertestapp.MainActivity: void sink(android.content.Intent)>')->Class('de.foellix.aql.slicer.slicertestapp.MainActivity')->App('example/debug.apk') ! }) ?\"");
	}

	@Test
	public void test10() {
		test("-d debug -m slice -from \"Statement('$r5 = virtualinvoke $r4.<android.telephony.TelephonyManager: java.lang.String getSimSerialNumber()>()')->Method('<de.foellix.aql.slicer.slicertestapp.MainActivity: void onCreate(android.os.Bundle)>')->Class('de.foellix.aql.slicer.slicertestapp.MainActivity')->App('example/debug.apk')\" -to \"Statement('%STATEMENT_FROM%')->Method('<de.foellix.aql.slicer.slicertestapp.MainActivity: void sink(android.content.Intent)>')->Class('de.foellix.aql.slicer.slicertestapp.MainActivity')->App('example/debug.apk') ! }) ?\"");
	}

	@Test
	public void test11() {
		test("-d debug -m slice -si -from \"Statement('getSimSerialNumber')->App('example/debug.apk')\" -to \"Statement('getSimSerialNumber')->App('example/debug.apk')\"");
	}

	@Test
	public void test12() {
		test("-d detailed -m slice -incomplete -from \"Statement('$r7 = virtualinvoke $r6.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.MainActivity: void onCreate(android.os.Bundle)>')->Class('de.ecspride.MainActivity')->App('example/DroidBench/Lifecycle/BroadcastReceiverLifecycle2.apk')\" -to \"Statement('staticinvoke <android.util.Log: int d(java.lang.String,java.lang.String)>(#QUOTE#DroidBench#QUOTE#, $r3)')->Method('<de.ecspride.MainActivity$MyReceiver: void onReceive(android.content.Context,android.content.Intent)>')->Class('de.ecspride.MainActivity$MyReceiver')->App('example/DroidBench/Lifecycle/BroadcastReceiverLifecycle2.apk')\"");
	}

	@Test
	public void test13() {
		test("-d detailed -m slice -f none -incomplete -from \"Statement('$r7 = virtualinvoke $r6.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.MainActivity: void onCreate(android.os.Bundle)>')->Class('de.ecspride.MainActivity')->App('example/DroidBench/Lifecycle/BroadcastReceiverLifecycle2.apk')\" -to \"Statement('staticinvoke <android.util.Log: int d(java.lang.String,java.lang.String)>(#QUOTE#DroidBench#QUOTE#, $r3)')->Method('<de.ecspride.MainActivity$MyReceiver: void onReceive(android.content.Context,android.content.Intent)>')->Class('de.ecspride.MainActivity$MyReceiver')->App('example/DroidBench/Lifecycle/BroadcastReceiverLifecycle2.apk')\"");
	}

	@Test
	public void test14() {
		test("-d detailed -m slice -f jimple -incomplete -from \"Statement('$r7 = virtualinvoke $r6.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.MainActivity: void onCreate(android.os.Bundle)>')->Class('de.ecspride.MainActivity')->App('example/DroidBench/Lifecycle/BroadcastReceiverLifecycle2.apk')\" -to \"Statement('staticinvoke <android.util.Log: int d(java.lang.String,java.lang.String)>(#QUOTE#DroidBench#QUOTE#, $r3)')->Method('<de.ecspride.MainActivity$MyReceiver: void onReceive(android.content.Context,android.content.Intent)>')->Class('de.ecspride.MainActivity$MyReceiver')->App('example/DroidBench/Lifecycle/BroadcastReceiverLifecycle2.apk')\"");
	}

	@Test
	public void test15() {
		test("-d detailed -m slice -incomplete -from \"Statement('$r9 = virtualinvoke $r8.<android.net.wifi.WifiInfo: java.lang.String getMacAddress()>()', 147)->Method('<com.savemebeta.SCHKMS: void onCreate()>')->Class('com.savemebeta.SCHKMS')->App('example/TaintBench/save_me.apk')\" -to \"Statement('virtualinvoke $r7.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>($r5, null, $r6, null, null)', 333)->Method('<com.savemebeta.SCHKMS: void fetchContacts()>')->Class('com.savemebeta.SCHKMS')->App('example/TaintBench/save_me.apk')\"");
	}

	@Test
	public void test16() {
		test("-d detailed -m slice -f jimple -incomplete -from \"Statement('$r9 = virtualinvoke $r8.<android.net.wifi.WifiInfo: java.lang.String getMacAddress()>()', 147)->Method('<com.savemebeta.SCHKMS: void onCreate()>')->Class('com.savemebeta.SCHKMS')->App('example/TaintBench/save_me.apk')\" -to \"Statement('virtualinvoke $r7.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>($r5, null, $r6, null, null)', 333)->Method('<com.savemebeta.SCHKMS: void fetchContacts()>')->Class('com.savemebeta.SCHKMS')->App('example/TaintBench/save_me.apk')\"");
	}

	@Test
	public void test17() {
		test("-d detailed -m slice -f none -incomplete -from \"Statement('$r9 = virtualinvoke $r8.<android.net.wifi.WifiInfo: java.lang.String getMacAddress()>()', 147)->Method('<com.savemebeta.SCHKMS: void onCreate()>')->Class('com.savemebeta.SCHKMS')->App('example/TaintBench/save_me.apk')\" -to \"Statement('virtualinvoke $r7.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>($r5, null, $r6, null, null)', 333)->Method('<com.savemebeta.SCHKMS: void fetchContacts()>')->Class('com.savemebeta.SCHKMS')->App('example/TaintBench/save_me.apk')\"");
	}

	@Test
	public void test18() {
		test("-d detailed -m slice -f none -incomplete -from \"Statement('$d0 = virtualinvoke $r1.<android.location.Location: double getLongitude()>()')->Method('<com.example.location_broadcast.location_broadcast: void onLocationChanged(android.location.Location)>')->Class('com.example.location_broadcast.location_broadcast')->App('example/DroidBench/InterAppCommunication/Location_leakage/Location_Broadcast1.apk')\" -to App('%APP_APK_TO%')");
	}

	@Test
	public void test19() {
		test("-d detailed -f none -m sliceout -from \"Statement('$r4 = virtualinvoke $r3.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.InactiveActivity: void onCreate(android.os.Bundle)>')->Class('de.ecspride.InactiveActivity')->App('example/DroidBench/AndroidSpecific/InactiveActivity.apk')\" -to \"Statement('staticinvoke <android.util.Log: int i(java.lang.String,java.lang.String)>(#QUOTE#INFO#QUOTE#, $r4)')->Method('<de.ecspride.InactiveActivity: void onCreate(android.os.Bundle)>')->Class('de.ecspride.InactiveActivity')->App('example/DroidBench/AndroidSpecific/InactiveActivity.apk')\"");
	}

	private void test(String runCmd) {
		boolean noException = true;

		try {
			final String[] args = Helper.getRunCommandAsArray(runCmd);
			new Jicer(args).jice();
		} catch (final Exception e) {
			noException = false;
			e.printStackTrace();
		}

		assertTrue(noException);
	}
}
