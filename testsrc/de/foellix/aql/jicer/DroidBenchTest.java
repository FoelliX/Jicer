package de.foellix.aql.jicer;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import de.foellix.aql.system.BackupAndReset;

@Tag("systemIsSetup")
public class DroidBenchTest {
	@BeforeAll
	public static void before() {
		JicerAndAnalysisTest.doAnalysis = true;
		// JicerAndAnalysisTest.debug = "short";
		// JicerAndAnalysisTest.debug = "normal";
		JicerAndAnalysisTest.debug = "debug";
		// JicerAndAnalysisTest.debug = "detailed";
		BackupAndReset.resetOutputDirectories();
	}

	@Test
	public void test01() {
		// DirectLeak1
		final String apk = "example/DroidBench/AndroidSpecific/DirectLeak1.apk";
		JicerAndAnalysisTest.test(new File(apk),
				"-d " + JicerAndAnalysisTest.debug + " -m slice -si -from \"Statement('getDeviceId')->App('" + apk
						+ "')\" -to \"Statement('sendTextMessage')->App('" + apk + "')\"",
				false);
	}

	@Test
	public void test02() {
		// FlowSensitivity1
		final String apk = "example/DroidBench/Aliasing/FlowSensitivity1.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r4 = virtualinvoke $r3.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.MainActivity: void aliasFlowTest()>')->Class('de.ecspride.MainActivity')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r5.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>(#QUOTE#+49 1234#QUOTE#, null, $r4, null, null)')->Method('<de.ecspride.MainActivity: void aliasFlowTest()>')->Class('de.ecspride.MainActivity')->App('"
				+ apk + "')\"", false);
	}

	@Test
	public void test03() {
		// Library2
		final String apk = "example/DroidBench/AndroidSpecific/Library2.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r4 = virtualinvoke $r3.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.LibClass: java.lang.String getIMEI(android.content.Context)>')->Class('de.ecspride.LibClass')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r4.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>(#QUOTE#+49#QUOTE#, null, $r3, null, null)')->Method('<de.ecspride.MainActivity: void onCreate(android.os.Bundle)>')->Class('de.ecspride.MainActivity')->App('"
				+ apk + "')\"", true);
	}

	@Test
	public void test04() {
		// ArrayAccess4
		final String apk = "example/DroidBench/ArraysAndLists/ArrayAccess4.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r7 = virtualinvoke $r6.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.ArrayAccess4: void onCreate(android.os.Bundle)>')->Class('de.ecspride.ArrayAccess4')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r9.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>(#QUOTE#+49 1234#QUOTE#, null, $r7, null, null)')->Method('<de.ecspride.ArrayAccess4: void onCreate(android.os.Bundle)>')->Class('de.ecspride.ArrayAccess4')->App('"
				+ apk + "')\"", false);
	}

	@Test
	public void test05() {
		// ArrayAccess4
		final String apk = "example/DroidBench/ArraysAndLists/ArrayAccess4.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r7 = virtualinvoke $r6.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.ArrayAccess4: void onCreate(android.os.Bundle)>')->Class('de.ecspride.ArrayAccess4')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r9.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>(#QUOTE#+49 1234#QUOTE#, null, $r7, null, null)')->Method('<de.ecspride.ArrayAccess4: void onCreate(android.os.Bundle)>')->Class('de.ecspride.ArrayAccess4')->App('"
				+ apk + "')\"", false);
	}

	@Test
	public void test06() {
		// ArrayAccess4 (20)
		final String apk = "example/DroidBench/ArraysAndLists/ArrayAccess4.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r7 = virtualinvoke $r6.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.ArrayAccess4: void onCreate(android.os.Bundle)>')->Class('de.ecspride.ArrayAccess4')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r9.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>(#QUOTE#+49 1234#QUOTE#, null, $r7, null, null)')->Method('<de.ecspride.ArrayAccess4: void onCreate(android.os.Bundle)>')->Class('de.ecspride.ArrayAccess4')->App('"
				+ apk + "')\"", false);
	}

	@Test
	public void test07() {
		// ArrayAccess3 (19)
		final String apk = "example/DroidBench/ArraysAndLists/ArrayAccess3.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r5 = virtualinvoke $r4.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.ArrayAccess3: void onCreate(android.os.Bundle)>')->Class('de.ecspride.ArrayAccess3')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r6.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>(#QUOTE#+49 1234#QUOTE#, null, $r5, null, null)')->Method('<de.ecspride.ArrayAccess3: void onCreate(android.os.Bundle)>')->Class('de.ecspride.ArrayAccess3')->App('"
				+ apk + "')\"", true);
	}

	@Test
	public void test08() {
		// MultidimensionalArray1 (26)
		final String apk = "example/DroidBench/ArraysAndLists/MultidimensionalArray1.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -dg -m slice -from \"Statement('$r5 = virtualinvoke $r4.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<edu.mit.array_slice.MainActivity: void onCreate(android.os.Bundle)>')->Class('edu.mit.array_slice.MainActivity')->App('"
				+ apk
				+ "')\" -to \"Statement('staticinvoke <android.util.Log: int i(java.lang.String,java.lang.String)>(#QUOTE#DroidBench#QUOTE#, $r5)')->Method('<edu.mit.array_slice.MainActivity: void onCreate(android.os.Bundle)>')->Class('edu.mit.array_slice.MainActivity')->App('"
				+ apk + "')\"", true);
	}

	@Test
	public void test09() {
		// Button3
		final String apk = "example/DroidBench/Callbacks/Button3.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r5 = virtualinvoke $r4.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.Button1Listener: void onClick(android.view.View)>')->Class('de.ecspride.Button1Listener')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r2.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>(#QUOTE#+49#QUOTE#, null, $r4, null, null)')->Method('<de.ecspride.Button2Listener: void onClick(android.view.View)>')->Class('de.ecspride.Button2Listener')->App('"
				+ apk + "')\"", true);
	}

	@Test
	public void test10() {
		// FieldSensitivity3
		final String apk = "example/DroidBench/FieldAndObjectSensitivity/FieldSensitivity3.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r5 = virtualinvoke $r4.<android.telephony.TelephonyManager: java.lang.String getSimSerialNumber()>()')->Method('<de.ecspride.FieldSensitivity3: void onCreate(android.os.Bundle)>')->Class('de.ecspride.FieldSensitivity3')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r6.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>(#QUOTE#+49 1234#QUOTE#, null, $r5, null, null)')->Method('<de.ecspride.FieldSensitivity3: void onCreate(android.os.Bundle)>')->Class('de.ecspride.FieldSensitivity3')->App('"
				+ apk + "')\"", true);
	}

	@Test
	public void test11() {
		// ActivityEventSequence3
		final String apk = "example/DroidBench/Lifecycle/ActivityEventSequence3.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r1 = virtualinvoke $r4.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<edu.uta.ActivityEventSequence3: void onUserLeaveHint()>')->Class('edu.uta.ActivityEventSequence3')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r4.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>($r5, null, $r1, null, null)')->Method('<edu.uta.ActivityEventSequence3: void onResume()>')->Class('edu.uta.ActivityEventSequence3')->App('"
				+ apk + "')\"", true, false, false);
	}

	@Test
	public void test12() {
		// AsyncTask1
		final String apk = "example/DroidBench/Threading/AsyncTask1.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r7 = virtualinvoke $r5.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.MainActivity: void onCreate(android.os.Bundle)>')->Class('de.ecspride.MainActivity')->App('"
				+ apk
				+ "')\" -to \"Statement('staticinvoke <android.util.Log: int d(java.lang.String,java.lang.String)>(#QUOTE#DroidBench#QUOTE#, $r2)')->Method('<de.ecspride.MainActivity$MyAsyncTask: java.lang.String doInBackground(java.lang.String[])>')->Class('de.ecspride.MainActivity$MyAsyncTask')->App('"
				+ apk + "')\"", true);
	}

	@Test
	public void test13() {
		// JavaThread1 (197)
		final String apk = "example/DroidBench/Threading/JavaThread1.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r6 = virtualinvoke $r4.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.MainActivity: void onCreate(android.os.Bundle)>')->Class('de.ecspride.MainActivity')->App('"
				+ apk
				+ "')\" -to \"Statement('staticinvoke <android.util.Log: int d(java.lang.String,java.lang.String)>(#QUOTE#DroidBench#QUOTE#, $r1)')->Method('<de.ecspride.MainActivity$MyThread: void run()>')->Class('de.ecspride.MainActivity$MyThread')->App('"
				+ apk + "')\"", true);
	}

	@Test
	public void test14() {
		// JavaThread2 (198)
		final String apk = "example/DroidBench/Threading/JavaThread2.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r5 = virtualinvoke $r4.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.MainActivity: void onCreate(android.os.Bundle)>')->Class('de.ecspride.MainActivity')->App('"
				+ apk
				+ "')\" -to \"Statement('staticinvoke <android.util.Log: int d(java.lang.String,java.lang.String)>(#QUOTE#DroidBench#QUOTE#, $r1)')->Method('<de.ecspride.MainActivity$1: void run()>')->Class('de.ecspride.MainActivity$1')->App('"
				+ apk + "')\"", true);
	}

	@Test
	public void test15() {
		// Executor1 (196)
		final String apk = "example/DroidBench/Threading/Executor1.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r7 = virtualinvoke $r4.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.MainActivity: void onCreate(android.os.Bundle)>')->Class('de.ecspride.MainActivity')->App('"
				+ apk
				+ "')\" -to \"Statement('staticinvoke <android.util.Log: int d(java.lang.String,java.lang.String)>(#QUOTE#DroidBench#QUOTE#, $r1)')->Method('<de.ecspride.MainActivity$MyRunnable: void run()>')->Class('de.ecspride.MainActivity$MyRunnable')->App('"
				+ apk + "')\"", true);
	}

	@Test
	public void test16() {
		// Looper1 (199)
		final String apk = "example/DroidBench/Threading/Looper1.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r11 = virtualinvoke $r10.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.MainActivity: void onCreate(android.os.Bundle)>')->Class('de.ecspride.MainActivity')->App('"
				+ apk
				+ "')\" -to \"Statement('staticinvoke <android.util.Log: int d(java.lang.String,java.lang.String)>(#QUOTE#DroidBench#QUOTE#, $r3)')->Method('<de.ecspride.LooperThread$1: void handleMessage(android.os.Message)>')->Class('de.ecspride.LooperThread$1')->App('"
				+ apk + "')\"", true);
	}

	@Test
	public void test17() {
		// InheritedObjects1 (68)
		final String apk = "example/DroidBench/FieldAndObjectSensitivity/InheritedObjects1.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r1 = virtualinvoke $r2.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.VarA: java.lang.String getInfo()>')->Class('de.ecspride.VarA')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r5.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>(#QUOTE#+49 1234#QUOTE#, null, $r6, null, null)')->Method('<de.ecspride.InheritedObjects1: void onCreate(android.os.Bundle)>')->Class('de.ecspride.InheritedObjects1')->App('"
				+ apk + "')\"", true);
	}

	@Test
	public void test18() {
		// VirtualDispatch2 (95)
		final String apk = "example/DroidBench/GeneralJava/VirtualDispatch2.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r1 = virtualinvoke $r2.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<edu.mit.dynamic_dispatch.B: java.lang.String f()>')->Class('edu.mit.dynamic_dispatch.B')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r8.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>(#QUOTE#+49 1234#QUOTE#, null, $r9, null, null)')->Method('<edu.mit.dynamic_dispatch.MainActivity: void onCreate(android.os.Bundle)>')->Class('edu.mit.dynamic_dispatch.MainActivity')->App('"
				+ apk + "')\"", true);
	}

	@Test
	public void test19() {
		// UnreachableBoth (202)
		final String apk = "example/DroidBench/UnreachableCode/UnreachableBoth.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r5 = virtualinvoke $r4.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<edu.wayne.cs.MainActivity: void onCreate(android.os.Bundle)>')->Class('edu.wayne.cs.MainActivity')->App('"
				+ apk
				+ "')\" -to \"Statement('lookupswitch($i0) { case 21: goto staticinvoke <android.util.Log: int d(java.lang.String,java.lang.String)>(#QUOTE#DroidBench#QUOTE#, $r5); default: goto return; }')->Method('<edu.wayne.cs.MainActivity: void onCreate(android.os.Bundle)>')->Class('edu.wayne.cs.MainActivity')->App('"
				+ apk + "')\"", true);
	}

	@Test
	public void test20() {
		// UnreachableSink1 (203)
		final String apk = "example/DroidBench/UnreachableCode/UnreachableSink1.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r5 = virtualinvoke $r4.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<edu.wayne.cs.MainActivity: void onCreate(android.os.Bundle)>')->Class('edu.wayne.cs.MainActivity')->App('"
				+ apk
				+ "')\" -to \"Statement('lookupswitch($i0) { case 21: goto staticinvoke <android.util.Log: int d(java.lang.String,java.lang.String)>(#QUOTE#DroidBench#QUOTE#, $r5); default: goto return; }')->Method('<edu.wayne.cs.MainActivity: void onCreate(android.os.Bundle)>')->Class('edu.wayne.cs.MainActivity')->App('"
				+ apk + "')\"", true);
	}

	@Test
	public void test21() {
		// ActivityLifecycle1 (143)
		final String apk = "example/DroidBench/LifeCycle/ActivityLifecycle1.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r4 = virtualinvoke $r3.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.ActivityLifecycle1: void onCreate(android.os.Bundle)>')->Class('de.ecspride.ActivityLifecycle1')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r4.<java.net.HttpURLConnection: void connect()>()')->Method('<de.ecspride.ActivityLifecycle1: void connect()>')->Class('de.ecspride.ActivityLifecycle1')->App('"
				+ apk + "')\"", true, false, false);
	}

	@Test
	public void test22() {
		// DeviceId_Broadcast1 (101)
		final String apk = "example/DroidBench/InterAppCommunication/Device_Id_leakage/DeviceId_Broadcast1.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r6 = virtualinvoke $r5.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<com.example.deviceid_broadcast.broadcast_deviceid: void onReceive(android.content.Context,android.content.Intent)>')->Class('com.example.deviceid_broadcast.broadcast_deviceid')->App('"
				+ apk + "')\"", true, false, false);
	}

	@Test
	public void test23() {
		// LocationLeak1 (36)
		final String apk = "example/DroidBench/Callbacks/LocationLeak1.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$d0 = virtualinvoke $r1.<android.location.Location: double getLatitude()>()')->Method('<de.ecspride.LocationLeak1$MyLocationListener: void onLocationChanged(android.location.Location)>')->Class('de.ecspride.LocationLeak1$MyLocationListener')->App('"
				+ apk
				+ "')\" -to \"Statement('staticinvoke <android.util.Log: int d(java.lang.String,java.lang.String)>(#QUOTE#Latitude#QUOTE#, $r2)')->Method('<de.ecspride.LocationLeak1: void onResume()>')->Class('de.ecspride.LocationLeak1')->App('"
				+ apk + "')\"", true);
	}
}