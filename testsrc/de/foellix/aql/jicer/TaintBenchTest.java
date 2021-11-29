package de.foellix.aql.jicer;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import de.foellix.aql.Log;
import de.foellix.aql.system.BackupAndReset;

@Tag("longLasting")
@Tag("systemIsSetup")
public class TaintBenchTest {
	@BeforeAll
	public static void before() {
		JicerAndAnalysisTest.doAnalysis = true;
		// JicerAndAnalysisTest.debug = "short";
		// JicerAndAnalysisTest.debug = "normal";
		JicerAndAnalysisTest.debug = "debug";
		// JicerAndAnalysisTest.debug = "detailed";
		Log.setShorten(true);
		BackupAndReset.resetOutputDirectories();
	}

	// @Test
	// Test fails -> Reason: Unknown. Repackaging this app with Soot but not changing the code at all has the same effect.
	public void test01() {
		// cajino_baidu
		final String apk = "example/TaintBench/cajino_baidu.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r5 = virtualinvoke $r2.<java.io.File: java.io.File[] listFiles()>()', 431)->Method('<ca.ji.no.method10.BaiduUtils$3: void run()>')->Class('ca.ji.no.method10.BaiduUtils$3')->App('"
				+ apk
				+ "')\" -to \"Statement('$r9 = virtualinvoke $r0.<com.baidu.inf.iis.bcs.BaiduBCS: com.baidu.inf.iis.bcs.response.BaiduBCSResponse putObject(com.baidu.inf.iis.bcs.request.PutObjectRequest)>($r6)', 464)->Method('<ca.ji.no.method10.BaiduUtils: void putObjectByInputStream(com.baidu.inf.iis.bcs.BaiduBCS,java.lang.String,java.lang.String)>')->Class('ca.ji.no.method10.BaiduUtils')->App('"
				+ apk + "')\"", true, true);
	}

	@Test
	public void test02() {
		// chat_hook (31)
		final String apk = "example/TaintBench/chat_hook.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r3 = virtualinvoke $r2.<android.content.Intent: java.lang.String getStringExtra(java.lang.String)>(#QUOTE#chat#QUOTE#)', 36)->Method('<com.example.chathook.HookReceiver: void onReceive(android.content.Context,android.content.Intent)>')->Class('com.example.chathook.HookReceiver')->App('"
				+ apk
				+ "')\" -to \"Statement('staticinvoke <android.util.Log: int i(java.lang.String,java.lang.String)>(#QUOTE#ligan#QUOTE#, #QUOTE#same msg #QUOTE#)', 39)->Method('<com.example.chathook.HookReceiver: void onReceive(android.content.Context,android.content.Intent)>')->Class('com.example.chathook.HookReceiver')->App('"
				+ apk + "')\"", true, true, false);
	}

	@Test
	public void test03() {
		// overlay_android_samp
		final String apk = "example/TaintBench/overlay_android_samp.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r3 = virtualinvoke $r0.<exts.whats.activities.Cards: android.view.View findViewById(int)>(2131492880)', 118)->Method('<exts.whats.activities.Cards: void onCreate(android.os.Bundle)>')->Class('exts.whats.activities.Cards')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r0.<exts.whats.activities.Cards: android.content.ComponentName startService(android.content.Intent)>($r3)', 322)->Method('<exts.whats.activities.Cards: void sendData()>')->Class('exts.whats.activities.Cards')->App('"
				+ apk + "')\"", true, true, false);
	}

	@Test
	public void test04() {
		// save_me (159)
		final String apk = "example/TaintBench/save_me.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r6 = virtualinvoke $r5.<android.widget.EditText: android.text.Editable getText()>()', 134)->Method('<com.savemebeta.Analyse$1: void onClick(android.view.View)>')->Class('com.savemebeta.Analyse$1')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r5.<android.content.ContentValues: void put(java.lang.String,java.lang.String)>(#QUOTE#user_name#QUOTE#, $r2)', 43)->Method('<com.savemebeta.DatabaseOperationslogin: void putInformation(com.savemebeta.DatabaseOperationslogin,java.lang.String,java.lang.String,java.lang.String)>')->Class('com.savemebeta.DatabaseOperationslogin')->App('"
				+ apk + "')\"", true, true);
	}

	// @Test
	// Test fails -> Reason: Custom callback not covered by "on"-approximation.
	public void test05_1() {
		// smssend_packageInstaller (187)
		final String apk = "example/TaintBench/smssend_packageInstaller.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r4 = virtualinvoke $r3.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()', 549)->Method('<ru.beta.Functions: java.lang.String getImei(android.content.Context)>')->Class('ru.beta.Functions')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r7.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>($r0, null, $r2, null, null)', 449)->Method('<ru.beta.Functions: boolean sendSms(java.lang.String,java.lang.String)>')->Class('ru.beta.Functions')->App('"
				+ apk + "')\"", true, true);
	}

	// @Test
	// Test fails -> Reason: (non-determinism) FlowDroid's analysis is sometimes successful and sometimes not. This holds for the original as well as for the sliced app.
	public void test05_2() {
		// smssend_packageInstaller (189)
		final String apk = "example/TaintBench/smssend_packageInstaller.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r4 = virtualinvoke $r3.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()', 280)->Method('<install.app.Settings: java.lang.String getImei(android.content.Context)>')->Class('install.app.Settings')->App('"
				+ apk
				+ "')\" -to \"Statement('$r9 = virtualinvoke $r8.<java.net.HttpURLConnection: java.io.OutputStream getOutputStream()>()', 430)->Method('<install.app.MainActivity: java.net.HttpURLConnection sendHttpRequest(java.lang.String,java.lang.String,java.util.List,java.util.List)>')->Class('install.app.MainActivity')->App('"
				+ apk + "')\"", true, true);
	}

	@Test
	public void test06() {
		// stels_flashplayer_android_update
		final String apk = "example/TaintBench/stels_flashplayer_android_update.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r4 = virtualinvoke $r3.<android.telephony.TelephonyManager: java.lang.String getSubscriberId()>()', 572)->Method('<ru.stels2.Functions: java.lang.String getImsi(android.content.Context)>')->Class('ru.stels2.Functions')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r6.<java.io.DataOutputStream: void write(byte[])>($r15)', 102)->Method('<ru.stels2.Functions: java.net.HttpURLConnection sendHttpRequest(java.lang.String,java.lang.String,java.util.List,java.util.List,boolean,java.lang.String)>')->Class('ru.stels2.Functions')->App('"
				+ apk + "')\"", true, true, false);
	}

	@Test
	public void test07() {
		// stels_flashplayer_android_update
		final String apk = "example/TaintBench/stels_flashplayer_android_update.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r3 = virtualinvoke $r2.<android.content.pm.PackageManager: java.util.List getInstalledPackages(int)>(0)', 618)->Method('<ru.stels2.Functions: java.util.List getInstalledAppList(android.content.Context)>')->Class('ru.stels2.Functions')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r6.<java.io.DataOutputStream: void write(byte[])>($r15)', 102)->Method('<ru.stels2.Functions: java.net.HttpURLConnection sendHttpRequest(java.lang.String,java.lang.String,java.util.List,java.util.List,boolean,java.lang.String)>')->Class('ru.stels2.Functions')->App('"
				+ apk + "')\"", true, true, false);
	}

	// @Test
	// Test fails -> Reason: FlowDroid does not generate dummy main for slicing.
	public void test08() {
		// samsapo
		final String apk = "example/TaintBench/samsapo.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r7 = staticinvoke <android.telephony.gsm.SmsMessage: android.telephony.gsm.SmsMessage createFromPdu(byte[])>($r6)', 30)->Method('<com.android.tools.system.SmsReceiver: void onReceive(android.content.Context,android.content.Intent)>')->Class('com.android.tools.system.SmsReceiver')->App('"
				+ apk
				+ "')\" -to \"Statement('$r11 = interfaceinvoke $r3.<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>($r8)', 43)->Method('<com.android.tools.system.MyPostRequest: java.lang.String doInBackground(java.util.ArrayList[])>')->Class('com.android.tools.system.MyPostRequest')->App('"
				+ apk + "')\"", true, true, true, "Amandroid");
	}

	// @Test
	// Test fails -> Reason: ICC-Edge to trigger flow
	public void test09() {
		// save_me (168)
		final String apk = "example/TaintBench/save_me.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r8 = virtualinvoke $r7.<android.telephony.TelephonyManager: java.lang.String getSimCountryIso()>()', 99)->Method('<com.savemebeta.CHECKUPD: void onCreate()>')->Class('com.savemebeta.CHECKUPD')->App('"
				+ apk
				+ "')\" -to \"Statement('interfaceinvoke $r4.<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>($r5)', 162)->Method('<com.savemebeta.CHECKUPD$sendmyinfos: java.lang.Void doInBackground(java.lang.Void[])>')->Class('com.savemebeta.CHECKUPD$sendmyinfos')->App('"
				+ apk + "')\"", true, true, true, "Amandroid");
	}

	@Test
	public void test10() {
		// jollyserv (72)
		final String apk = "example/TaintBench/jollyserv.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r8 = virtualinvoke $r7.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()', 36)->Method('<fm.xtube.CheckAgeActivity: void onCreate(android.os.Bundle)>')->Class('fm.xtube.CheckAgeActivity')->App('"
				+ apk
				+ "')\" -to \"Statement('$r8 = virtualinvoke $r7.<java.util.concurrent.ThreadPoolExecutor: java.util.concurrent.Future submit(java.lang.Runnable)>(r12)', 536)->Method('<com.loopj.android.http.AsyncHttpClient: void sendRequest(org.apache.http.impl.client.DefaultHttpClient,org.apache.http.protocol.HttpContext,org.apache.http.client.methods.HttpUriRequest,java.lang.String,com.loopj.android.http.AsyncHttpResponseHandler,android.content.Context)>')->Class('com.loopj.android.http.AsyncHttpClient')->App('"
				+ apk + "')\"", true, true, true);
	}

	@Test
	public void test11() {
		// phospy (98)
		final String apk = "example/TaintBench/phospy.apk";
		System.out.println(
				" -m slice -from \"Statement('$r10 = virtualinvoke $r9.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()', 92)->Method('<com.labado.lulaoshi.myService: void log()>')->Class('com.labado.lulaoshi.myService')->App('"
						+ apk
						+ "')\" -to \"Statement('virtualinvoke $r4.<java.io.DataOutputStream: void writeUTF(java.lang.String)>($r15)', 113)->Method('<com.labado.lulaoshi.myService: void log()>')->Class('com.labado.lulaoshi.myService')->App('"
						+ apk + "')\"");
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r10 = virtualinvoke $r9.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()', 92)->Method('<com.labado.lulaoshi.myService: void log()>')->Class('com.labado.lulaoshi.myService')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r4.<java.io.DataOutputStream: void writeUTF(java.lang.String)>($r15)', 113)->Method('<com.labado.lulaoshi.myService: void log()>')->Class('com.labado.lulaoshi.myService')->App('"
				+ apk + "')\"", true, true, true);
	}

	@Test
	public void test12() {
		// roidsec (140)
		final String apk = "example/TaintBench/roidsec.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r4 = virtualinvoke $r2.<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)>($r3, null, null, null, null)', 682)->Method('<cn.phoneSync.PhoneSyncService: java.lang.String getContactInfo()>')->Class('cn.phoneSync.PhoneSyncService')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r8.<java.io.OutputStream: void write(byte[])>($r12)', 173)->Method('<cn.phoneSync.PhoneSyncService: void BackConnTask()>')->Class('cn.phoneSync.PhoneSyncService')->App('"
				+ apk + "')\"", true, true, false);
	}

	@Test
	public void test13() {
		// xbot_android_samp (221)
		final String apk = "example/TaintBench/xbot_android_samp.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r10 = staticinvoke <android.telephony.SmsMessage: android.telephony.SmsMessage createFromPdu(byte[])>($r9)', 30)->Method('<com.address.core.SMSHandler: void onReceive(android.content.Context,android.content.Intent)>')->Class('com.address.core.SMSHandler')->App('"
				+ apk
				+ "')\" -to \"Statement('$r5 = interfaceinvoke $r7.<org.mozilla.javascript.Function: java.lang.Object call(org.mozilla.javascript.Context,org.mozilla.javascript.Scriptable,org.mozilla.javascript.Scriptable,java.lang.Object[])>($r10, $r4, $r3, $r2)', 90)->Method('<com.address.core.Script: java.lang.Object call(java.lang.String,java.lang.Object[])>')->Class('com.address.core.Script')->App('"
				+ apk + "')\"", true, true, false);
	}

	@Test
	public void test14() {
		// hummingbad_android_samp (70)
		final String apk = "example/TaintBench/hummingbad_android_samp.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r11 = interfaceinvoke $r9.<android.database.Cursor: java.lang.String getString(int)>($i0)', 115)->Method('<com.android.ad.du.BaiduCacheDBUtil: java.util.ArrayList listDuAdDatasFromAppCache(android.content.Context)>')->Class('com.android.ad.du.BaiduCacheDBUtil')->App('"
				+ apk
				+ "')\" -to \"Statement('$l1 = virtualinvoke $r6.<android.database.sqlite.SQLiteDatabase: long insert(java.lang.String,java.lang.String,android.content.ContentValues)>(#QUOTE#tb_dubai_adv#QUOTE#, null, $r2)', 120)->Method('<com.mb.bdapp.db.DBService: long insertAd(com.mb.bdapp.db.DuAd)>')->Class('com.mb.bdapp.db.DBService')->App('"
				+ apk + "')\"", true, true, false);
	}

	@Test
	public void test15() {
		// proxy_samp (114)
		final String apk = "example/TaintBench/proxy_samp.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r9 = virtualinvoke $r8.<android.accounts.AccountManager: android.accounts.Account[] getAccounts()>()', 320)->Method('<com.smart.studio.proxy.ProxyService: void onCreate()>')->Class('com.smart.studio.proxy.ProxyService')->App('"
				+ apk
				+ "')\" -to \"Statement('staticinvoke <android.util.Log: int i(java.lang.String,java.lang.String)>(#QUOTE#proxy#QUOTE#, $r3)', 268)->Method('<com.smart.studio.proxy.ProxyService: void LogFile(java.lang.String)>')->Class('com.smart.studio.proxy.ProxyService')->App('"
				+ apk + "')\"", true, true, true);
	}

	@Test
	public void test16() {
		// remote_control_smack (121)
		final String apk = "example/TaintBench/remote_control_smack.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r6 = virtualinvoke $r4.<android.location.LocationManager: android.location.Location getLastKnownLocation(java.lang.String)>(#QUOTE#network#QUOTE#)', 81)->Method('<com.android.service.view.GPSTracker: android.location.Location getLocation()>')->Class('com.android.service.view.GPSTracker')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r1.<java.io.FileWriter: java.io.Writer append(java.lang.CharSequence)>($r6)', 129)->Method('<com.android.service.AlarmReceiver$1$1: void run()>')->Class('com.android.service.AlarmReceiver$1$1')->App('"
				+ apk + "')\"", true, true, false);
	}

	@Test
	public void test17() {
		// beita_com_beita_contact (13)
		final String apk = "example/TaintBench/beita_com_beita_contact.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('specialinvoke $r8.<java.io.FileInputStream: void <init>(java.lang.String)>($r4)', 43)->Method('<com.beita.contact.UploadUtil: void uploadFile()>')->Class('com.beita.contact.UploadUtil')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r5.<java.io.DataOutputStream: void write(byte[],int,int)>($r9, 0, $i0)', 47)->Method('<com.beita.contact.UploadUtil: void uploadFile()>')->Class('com.beita.contact.UploadUtil')->App('"
				+ apk + "')\"", true, true, false);
	}

	@Test
	public void test18() {
		// fakedaum (59)
		final String apk = "example/TaintBench/fakedaum.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r3 = virtualinvoke $r2.<android.telephony.TelephonyManager: java.lang.String getSimSerialNumber()>()', 23)->Method('<com.mvlove.util.PhoneUtil: java.lang.String getImei(android.content.Context)>')->Class('com.mvlove.util.PhoneUtil')->App('"
				+ apk
				+ "')\" -to \"Statement('$r14 = interfaceinvoke $r13.<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>($r7)', 667)->Method('<com.mvlove.http.HttpWrapper: java.lang.Object post(org.apache.http.client.methods.HttpPost,java.lang.String,java.util.Map,java.lang.Class)>')->Class('com.mvlove.http.HttpWrapper')->App('"
				+ apk + "')\"", true, true, false);
	}

	@Test
	public void test19() {
		// fakeplay (61)
		final String apk = "example/TaintBench/fakeplay.apk";
		JicerAndAnalysisTest.test(new File(apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -from \"Statement('$r14 = virtualinvoke $r12.<android.telephony.TelephonyManager: java.lang.String getNetworkOperatorName()>()', 124)->Method('<com.googleprojects.mm.JHService: void smsReceived(android.content.Intent)>')->Class('com.googleprojects.mm.JHService')->App('"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r1.<java.io.OutputStream: void write(byte[],int,int)>($r2, 0, $i0)', 322)->Method('<javax.activation.DataHandler: void writeTo(java.io.OutputStream)>')->Class('javax.activation.DataHandler')->App('"
				+ apk + "')\"", true, true, false);
	}
}