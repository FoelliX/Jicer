package de.foellix.aql.jicer;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

import de.foellix.aql.Log;
import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.system.AQLSystem;
import de.foellix.aql.system.BackupAndReset;
import de.foellix.aql.system.Options;

@Tag("systemIsSetup")
public class TaintBenchICCTest {
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
	// No test - just used for the creation of example/TaintBench/*.xml
	public void createTest() {
		final String app = "TaintBench/save_me";
		BackupAndReset.reset();
		final AQLSystem aqlSystem = new AQLSystem(
				new Options().setConfig(new File("D:/GIT-fpauck/AQL/config_jicer_run_ex.xml")));
		final Answer a = (Answer) aqlSystem.queryAndWait("CONNECT [ IntentSources IN App('example/" + app
				+ ".apk') ?, IntentSinks IN App('example/" + app + ".apk') ? ] ?").iterator().next();
		AnswerHandler.createXML(a, new File("example/" + app + ".xml"));
	}

	// @Test
	// Reason: No ICC edge available
	public void test01() {
		// chat_hook (31) + ICC
		final String apk = "TaintBench/chat_hook.apk";
		JicerAndAnalysisTest.test(new File("example", apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -ie \"example/chat_hook.xml\" -from \"Statement('$r3 = virtualinvoke $r2.<android.content.Intent: java.lang.String getStringExtra(java.lang.String)>(#QUOTE#chat#QUOTE#)', 36)->Method('<com.example.chathook.HookReceiver: void onReceive(android.content.Context,android.content.Intent)>')->Class('com.example.chathook.HookReceiver')->App('example/"
				+ apk
				+ "')\" -to \"Statement('staticinvoke <android.util.Log: int i(java.lang.String,java.lang.String)>(#QUOTE#ligan#QUOTE#, #QUOTE#same msg #QUOTE#)', 39)->Method('<com.example.chathook.HookReceiver: void onReceive(android.content.Context,android.content.Intent)>')->Class('com.example.chathook.HookReceiver')->App('example/"
				+ apk + "')\"", true, true, false, "Amandroid");
	}

	// @Test
	// Reason: No ICC edge available
	public void test02() {
		// save_me (160)
		final String apk = "TaintBench/save_me.apk";
		JicerAndAnalysisTest.test(new File("example", apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -ie \"example/save_me.xml\" -from \"Statement('$r6 = virtualinvoke $r5.<android.widget.EditText: android.text.Editable getText()>()', 134)->Method('<com.savemebeta.Analyse$1: void onClick(android.view.View)>')->Class('com.savemebeta.Analyse$1')->App('example/"
				+ apk
				+ "')\" -to \"Statement('virtualinvoke $r5.<android.content.ContentValues: void put(java.lang.String,java.lang.String)>(#QUOTE#user_number#QUOTE#, $r3)', 44)->Method('<com.savemebeta.DatabaseOperationslogin: void putInformation(com.savemebeta.DatabaseOperationslogin,java.lang.String,java.lang.String,java.lang.String)>')->Class('com.savemebeta.DatabaseOperationslogin')->App('example/"
				+ apk + "')\"", true, true, false, "Amandroid");
	}

	// @Test
	// Reason: No ICC edge available
	public void test03() {
		// save_me (168)
		final String apk = "TaintBench/save_me.apk";
		JicerAndAnalysisTest.test(new File("example", apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -ie \"example/save_me.xml\" -from \"Statement('$r8 = virtualinvoke $r7.<android.telephony.TelephonyManager: java.lang.String getSimCountryIso()>()', 99)->Method('<com.savemebeta.CHECKUPD: void onCreate()>')->Class('com.savemebeta.CHECKUPD')->App('example/"
				+ apk
				+ "')\" -to \"Statement('interfaceinvoke $r4.<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>($r5)', 162)->Method('<com.savemebeta.CHECKUPD$sendmyinfos: java.lang.Void doInBackground(java.lang.Void[])>')->Class('com.savemebeta.CHECKUPD$sendmyinfos')->App('example/"
				+ apk + "')\"", true, true, false, "Amandroid");
	}

	// @Test
	// Reason: No ICC edge available
	public void test04() {
		// save_me (171)
		final String apk = "TaintBench/save_me.apk";
		JicerAndAnalysisTest.test(new File("example", apk), "-d " + JicerAndAnalysisTest.debug
				+ " -m slice -ie \"example/save_me.xml\" -from \"Statement('$r8 = virtualinvoke $r13.<android.net.wifi.WifiInfo: java.lang.String getMacAddress()>()', 115)->Method('<com.savemebeta.CHECKUPD: void onCreate()>')->Class('com.savemebeta.CHECKUPD')->App('example/"
				+ apk
				+ "')\" -to \"Statement('interfaceinvoke $r4.<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>($r5)', 162)->Method('<com.savemebeta.CHECKUPD$sendmyinfos: java.lang.Void doInBackground(java.lang.Void[])>')->Class('com.savemebeta.CHECKUPD$sendmyinfos')->App('example/"
				+ apk + "')\"", true, true, false, "Amandroid");
	}
}